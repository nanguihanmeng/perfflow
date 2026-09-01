package com.perfflow.module.auth.service;
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
import java.time.LocalDateTime;
// 认证服务：登录、改密、资料修改、令牌刷新。
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    // 登录认证：校验账号密码并签发令牌，令牌版本号自增实现多设备互踢。
    @Transactional

    // 登录认证并签发令牌
    public LoginResp login(LoginReq req) {

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
            // 生成令牌
            String access = jwtUtil.generateAccess(u.getId(), u.getUsername(), u.getRole(),
                    u.getDeptId(), u.getDeptLead(), u.getMustChangePassword(), tokenVersion);
            // 生成令牌
            String refresh = jwtUtil.generateRefresh(u.getId(), u.getUsername(), u.getRole(), tokenVersion);
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
            resp.setMustChangePassword(u.getMustChangePassword());

            // 非空才处理
            if (u.getDeptId() != null) {

                // 查询单条
                SysDepartment d = deptMapper.selectById(u.getDeptId());
                if (d != null) resp.setDeptName(d.getName());
            }

            log.info("登录成功: userId={}, username={}, role={}", u.getId(), u.getUsername(), u.getRole());
            // 返回结果
            return resp;

        } catch (BadCredentialsException e) {

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
    public LoginResp changePassword(String newPassword) {

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
        LoginResp resp = new LoginResp();
        resp.setAccessToken(access);
        resp.setRefreshToken(refresh);
        resp.setExpiresIn(jwtUtil.getAccessTtl());
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole());
        resp.setDeptId(user.getDeptId());
        resp.setDeptLead(user.getDeptLead());
        // 标记需改密
        resp.setMustChangePassword(false);

        // 非空才处理
        if (user.getDeptId() != null) {

            // 查询单条
            SysDepartment d = deptMapper.selectById(user.getDeptId());
            if (d != null) resp.setDeptName(d.getName());
        }

        log.info("修改密码成功: userId={}", userId);
        // 返回结果
        return resp;
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
            LoginResp resp = new LoginResp();
            resp.setAccessToken(access);
            resp.setExpiresIn(jwtUtil.getAccessTtl());
            resp.setUserId(u.getId());
            resp.setUsername(u.getUsername());
            resp.setRealName(u.getRealName());
            resp.setRole(u.getRole());
            resp.setDeptId(u.getDeptId());
            resp.setDeptLead(u.getDeptLead());
            // 标记需改密
            resp.setMustChangePassword(u.getMustChangePassword());

            // 非空才处理
            if (u.getDeptId() != null) {

                // 查询单条
                SysDepartment d = deptMapper.selectById(u.getDeptId());
                if (d != null) resp.setDeptName(d.getName());
            }

            // 返回结果
            return resp;

        } catch (JwtException e) {

            // 校验失败抛异常
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }
    }
}
