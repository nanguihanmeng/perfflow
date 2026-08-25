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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResp login(LoginReq req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
            SysUser u = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                            .eq("username", req.getUsername()));
            if (u == null) {
                throw new BizException(ResultCode.LOGIN_INVALID);
            }
            // 记 last_login_at + 令牌版本号自增（新登录使旧设备 token 失效，实现互踢）
            u.setLastLoginAt(LocalDateTime.now());
            int tokenVersion = (u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1;
            u.setTokenVersion(tokenVersion);
            userMapper.updateById(u);

            String access = jwtUtil.generateAccess(u.getId(), u.getUsername(), u.getRole(),
                    u.getDeptId(), u.getDeptLead(), u.getMustChangePassword(), tokenVersion);
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
            resp.setMustChangePassword(u.getMustChangePassword());
            if (u.getDeptId() != null) {
                SysDepartment d = deptMapper.selectById(u.getDeptId());
                if (d != null) resp.setDeptName(d.getName());
            }
            log.info("登录成功: userId={}, username={}, role={}", u.getId(), u.getUsername(), u.getRole());
            return resp;
        } catch (BadCredentialsException e) {
            log.warn("登录失败（密码错误）: username={}", req.getUsername());
            throw new BizException(ResultCode.LOGIN_INVALID);
        } catch (DisabledException e) {
            log.warn("登录失败（账号禁用）: username={}", req.getUsername());
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
    }

    /**
     * 修改当前登录用户的密码，并清除强制改密标记。
     *
     * @param newPassword 新密码（明文，入库前 BCrypt 加密）
     */
    @Transactional
    public void changePassword(String newPassword) {
        Long userId = DataScopeContext.currentUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userMapper.updateById(user);
        log.info("修改密码成功: userId={}", userId);
    }

    /**
     * 修改当前登录用户的个人资料（姓名/邮箱/电话）。
     *
     * <p>绩效考核管理员账号信息变更的唯一渠道：登录后自行修改。
     *
     * @param req 资料请求
     */
    @Transactional
    public void updateProfile(ProfileReq req) {
        Long userId = DataScopeContext.currentUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        userMapper.updateById(user);
        log.info("修改个人资料: userId={}", userId);
    }

    public LoginResp refresh(String refreshToken) {
        try {
            Claims c = jwtUtil.parse(refreshToken);
            String type = c.get("type", String.class);
            if (!"refresh".equals(type)) {
                throw new BizException(ResultCode.TOKEN_INVALID, "非 refresh token");
            }
            Long userId = Long.valueOf(c.getSubject());
            SysUser u = userMapper.selectById(userId);
            if (u == null) throw new BizException(ResultCode.TOKEN_INVALID);
            // 刷新沿用当前令牌版本号（不改变版本，避免刷新导致旧 token 互踢）
            Integer tokenVersion = u.getTokenVersion() == null ? 0 : u.getTokenVersion();
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
            resp.setMustChangePassword(u.getMustChangePassword());
            if (u.getDeptId() != null) {
                SysDepartment d = deptMapper.selectById(u.getDeptId());
                if (d != null) resp.setDeptName(d.getName());
            }
            return resp;
        } catch (JwtException e) {
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }
    }
}
