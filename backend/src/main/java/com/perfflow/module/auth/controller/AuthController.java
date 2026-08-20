package com.perfflow.module.auth.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.auth.dto.ChangePasswordReq;
import com.perfflow.module.auth.dto.LoginReq;
import com.perfflow.module.auth.dto.LoginResp;
import com.perfflow.module.auth.dto.RefreshReq;
import com.perfflow.module.auth.service.AuthService;
import com.perfflow.security.DataScopeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "鉴权")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 token")
    public Result<LoginResp> refresh(@Valid @RequestBody RefreshReq req) {
        return Result.ok(authService.refresh(req.getRefreshToken()));
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户")
    @PreAuthorize("isAuthenticated()")
    public Result<Object> me() {
        return Result.ok(DataScopeContext.current());
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码（登录后可调用）")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        authService.changePassword(req.getNewPassword());
        return Result.ok();
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（前端清 token 即可）")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> logout() {
        return Result.ok();
    }
}
