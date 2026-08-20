package com.perfflow.module.auth.service;

import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.auth.dto.LoginReq;
import com.perfflow.module.auth.dto.LoginResp;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import com.perfflow.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
            // 记 last_login_at
            u.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(u);

            String access = jwtUtil.generateAccess(u.getId(), u.getUsername(), u.getRole(),
                    u.getDeptId(), u.getDeptLead(), u.getMustChangePassword());
            String refresh = jwtUtil.generateRefresh(u.getId(), u.getUsername(), u.getRole());

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
            return resp;
        } catch (BadCredentialsException e) {
            throw new BizException(ResultCode.LOGIN_INVALID);
        } catch (DisabledException e) {
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
            String access = jwtUtil.generateAccess(u.getId(), u.getUsername(), u.getRole(),
                    u.getDeptId(), u.getDeptLead(), u.getMustChangePassword());
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
