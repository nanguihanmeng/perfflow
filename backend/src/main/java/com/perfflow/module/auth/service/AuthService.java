package com.perfflow.module.auth.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.auth.dto.LoginReq;
import com.perfflow.module.auth.dto.LoginResp;
import com.perfflow.module.auth.dto.ProfileReq;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import com.perfflow.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
// 认证服务：登录、改密、资料修改、令牌刷新。
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 密码错误最大次数，超过后临时锁定。 */
    private static final int MAX_LOGIN_FAILURES = 5;
    /** 锁定时长。 */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    // ponytail: 单机内存锁定，多实例部署需迁移至 Redis。
    // 失败仅统计库中真实账号（见 onLoginFailure），两处 map 大小上限≈启用账号数，不会无界增长。
    // 已知账号可被持续爆破造成临时 15 分钟锁定窗口（非永久）：彻底防御需按客户端 IP 计数或接入验证码，
    // 内网 + 反代后 IP 不可靠，暂不引入，升级路径=Redis 计数 + IP/验证码。
    private static final Map<String, LoginFail> LOGIN_FAILURES = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> LOCKED_UNTIL = new ConcurrentHashMap<>();

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    // 登录认证：校验账号密码并签发令牌，令牌版本号自增实现多设备互踢。
    @Transactional

    // 登录认证并签发令牌
    public LoginResp login(LoginReq req) {

        // 账号已被临时锁定则直接拒绝
        checkNotLocked(req.getUsername());

        try {

            // 认证校验
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
            // 查询单条
            SysUser u = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                            .eq("username", req.getUsername()));

            // 判空处理
            if (u == null) {

                // 校验失败抛异常
                throw new BizException(ResultCode.LOGIN_INVALID);
            }
            // 记 last_login_at + 令牌版本号自增（新登录使旧设备 token 失效，实现互踢）
            u.setLastLoginAt(LocalDateTime.now());
            // 令牌版本管理
            int tokenVersion = (u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1;
            // 令牌版本管理
            u.setTokenVersion(tokenVersion);
            // 更新记录
            userMapper.updateById(u);
            // 登录成功，清零失败计数
            onLoginSuccess(req.getUsername());
            // 生成令牌
            String access = jwtUtil.generateAccess(u.getId(), u.getUsername(), u.getRole(),
                    u.getDeptId(), u.getDeptLead(), u.getMustChangePassword(), tokenVersion);
            // 生成令牌
            String refresh = jwtUtil.generateRefresh(u.getId(), u.getUsername(), u.getRole(), tokenVersion);

            log.info("登录成功: userId={}, username={}, role={}", u.getId(), u.getUsername(), u.getRole());
            // 返回结果
            return buildLoginResp(u, access, refresh, u.getMustChangePassword());

        } catch (BadCredentialsException e) {

            // 记录失败次数，达到阈值即锁定
            if (onLoginFailure(req.getUsername())) {

                throw new BizException(ResultCode.ACCOUNT_LOCKED);
            }
            log.warn("登录失败（密码错误）: username={}", req.getUsername());
            // 校验失败抛异常
            throw new BizException(ResultCode.LOGIN_INVALID);

        } catch (DisabledException e) {

            log.warn("登录失败（账号禁用）: username={}", req.getUsername());
            // 校验失败抛异常
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
    }

    // 修改当前登录用户的密码，清除强制改密标记，并返回新令牌（改密后无需重新登录）。
     // 因此改密后前端必须用返回的新令牌覆盖本地登录态，才能正常访问业务页面。
    @Transactional
    // 修改当前登录用户密码并返回新令牌。

    // 修改密码并刷新令牌
    public LoginResp changePassword(String oldPassword, String newPassword) {

        // 取当前用户上下文
        Long userId = DataScopeContext.currentUserId();

        // 判空处理
        if (userId == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 查询单条
        SysUser user = userMapper.selectById(userId);

        // 判空处理
        if (user == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 校验原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "原密码错误");
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(newPassword));
        // 标记需改密
        user.setMustChangePassword(false);
        // 令牌版本号自增：改密前签发的旧令牌立即失效（与登录互踢机制一致）
        int tokenVersion = (user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1;
        // 令牌版本管理
        user.setTokenVersion(tokenVersion);
        // 更新记录
        userMapper.updateById(user);
        // 生成令牌
        String access = jwtUtil.generateAccess(user.getId(), user.getUsername(), user.getRole(),
                user.getDeptId(), user.getDeptLead(), false, tokenVersion);
        // 生成令牌
        String refresh = jwtUtil.generateRefresh(user.getId(), user.getUsername(), user.getRole(), tokenVersion);

        log.info("修改密码成功: userId={}", userId);
        // 返回结果
        return buildLoginResp(user, access, refresh, false);
    }

    // 修改当前登录用户的个人资料（姓名/邮箱/电话）。
    @Transactional
    // 修改当前登录用户个人资料。

    // 更新个人资料
    public void updateProfile(ProfileReq req) {

        // 取当前用户上下文
        Long userId = DataScopeContext.currentUserId();

        // 判空处理
        if (userId == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        // 查询单条
        SysUser user = userMapper.selectById(userId);

        // 判空处理
        if (user == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.NOT_FOUND);
        }

        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        // 更新记录
        userMapper.updateById(user);
        log.info("修改个人资料: userId={}", userId);
    }

    // 刷新访问令牌，沿用当前令牌版本号。

    // 刷新访问令牌
    public LoginResp refresh(String refreshToken) {

        try {

            // 解析令牌
            Claims c = jwtUtil.parse(refreshToken);
            String type = c.get("type", String.class);

            // 相等判断
            if (!"refresh".equals(type)) {

                // 校验失败抛异常
                throw new BizException(ResultCode.TOKEN_INVALID, "非 refresh token");
            }

            Long userId = Long.valueOf(c.getSubject());
            // 查询单条
            SysUser u = userMapper.selectById(userId);
            if (u == null) throw new BizException(ResultCode.TOKEN_INVALID);
            // 刷新沿用当前令牌版本号（不改变版本，避免刷新导致旧 token 互踢）
            Integer tokenVersion = u.getTokenVersion() == null ? 0 : u.getTokenVersion();
            // 生成令牌
            String access = jwtUtil.generateAccess(u.getId(), u.getUsername(), u.getRole(),
                    u.getDeptId(), u.getDeptLead(), u.getMustChangePassword(), tokenVersion);

            // 返回结果
            return buildLoginResp(u, access, null, u.getMustChangePassword());

        } catch (JwtException e) {

            // 校验失败抛异常
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }
    }

    // ==================== 私有辅助 ====================

    // 组装登录响应：令牌 + 用户基础信息 + 部门名。
    private LoginResp buildLoginResp(SysUser u, String access, String refresh, boolean mustChange) {

        LoginResp resp = new LoginResp();
        resp.setAccessToken(access);
        resp.setRefreshToken(refresh);
        resp.setExpiresIn(jwtUtil.getAccessTtl());
        resp.setUserId(u.getId());
        resp.setUsername(u.getUsername());
        resp.setRealName(u.getRealName());
        resp.setRole(u.getRole());
        resp.setDeptId(u.getDeptId());
        resp.setDeptLead(u.getDeptLead());
        // 标记需改密
        resp.setMustChangePassword(mustChange);

        // 非空才处理
        if (u.getDeptId() != null) {

            // 查询单条
            SysDepartment d = deptMapper.selectById(u.getDeptId());
            if (d != null) resp.setDeptName(d.getName());
        }
        // 返回结果
        return resp;
    }

    // 账号未锁定则放行；锁定已到期时自动清除锁定与失败计数。
    private void checkNotLocked(String username) {

        // 锁定到期时间
        LocalDateTime until = LOCKED_UNTIL.get(username);
        // 判空处理
        if (until == null) {

            return;
        }
        // 值比较
        if (until.isAfter(LocalDateTime.now())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.ACCOUNT_LOCKED);
        }
        // 锁定到期，清除锁定与失败计数
        LOCKED_UNTIL.remove(username);
        LOGIN_FAILURES.remove(username);
    }

    // 登录成功，清零失败计数。
    private void onLoginSuccess(String username) {

        LOCKED_UNTIL.remove(username);
        LOGIN_FAILURES.remove(username);
    }

    // 登录失败计数 +1，达到阈值即锁定账号并返回 true。
    // 距上次失败超过锁定时长视为新一轮计数，偶发失败不会长期累积。
    private synchronized boolean onLoginFailure(String username) {

        // 仅统计真实存在的账号：伪造/随机用户名不落内存，避免 map 无界增长
        if (!accountExists(username)) {

            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LoginFail fail = LOGIN_FAILURES.get(username);
        // 判空处理
        if (fail == null || fail.lastAt == null || fail.lastAt.isBefore(now.minus(LOCK_DURATION))) {

            fail = new LoginFail();
            fail.count = 1;
        } else {

            fail.count++;
        }
        fail.lastAt = now;
        // 值比较
        if (fail.count >= MAX_LOGIN_FAILURES) {

            LOGIN_FAILURES.remove(username);
            LOCKED_UNTIL.put(username, now.plus(LOCK_DURATION));
            // 返回结果
            return true;
        }
        LOGIN_FAILURES.put(username, fail);
        // 返回结果
        return false;
    }

    // 账号是否存在于系统中（仅对真实账号做失败计数，内存占用上限=账号总数）。
    private boolean accountExists(String username) {

        if (username == null) {

            return false;
        }

        Long count = userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", username));
        return count != null && count > 0;
    }

    // 登录失败计数（含最近失败时间，超过静默期后重新计数）。
    private static final class LoginFail {

        private int count;
        private LocalDateTime lastAt;
    }
}
