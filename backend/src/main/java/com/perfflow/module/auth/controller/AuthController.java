package com.perfflow.module.auth.controller;
import com.perfflow.common.api.Result;
import com.perfflow.module.auth.dto.ChangePasswordReq;
import com.perfflow.module.auth.dto.LoginReq;
import com.perfflow.module.auth.dto.LoginResp;
import com.perfflow.module.auth.dto.ProfileReq;
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

    // 用户登录
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {

        // 返回成功响应
        return Result.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 token")

    // 刷新令牌
    public Result<LoginResp> refresh(@Valid @RequestBody RefreshReq req) {

        // 返回成功响应
        return Result.ok(authService.refresh(req.getRefreshToken()));
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户")
    @PreAuthorize("isAuthenticated()")
    // 执行 me。

    // 业务处理
    public Result<Object> me() {

        // 取当前用户上下文
        return Result.ok(DataScopeContext.current());
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码（登录后可调用，返回新令牌，改密后免重新登录）")
    @PreAuthorize("isAuthenticated()")
    // 修改当前登录用户密码并返回新令牌。

    // 业务处理
    public Result<LoginResp> changePassword(@Valid @RequestBody ChangePasswordReq req) {

        // 返回成功响应
        return Result.ok(authService.changePassword(req.getOldPassword(), req.getNewPassword()));
    }

    @PutMapping("/profile")
    @Operation(summary = "修改个人资料（姓名/邮箱/电话）")
    @PreAuthorize("isAuthenticated()")
    // 修改当前登录用户个人资料。

    // 更新数据
    public Result<Void> updateProfile(@Valid @RequestBody ProfileReq req) {

        // 调用业务服务
        authService.updateProfile(req);
        // 返回成功响应
        return Result.ok();
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（前端清 token 即可）")
    @PreAuthorize("isAuthenticated()")

    // 业务处理
    public Result<Void> logout() {

        // 返回成功响应
        return Result.ok();
    }
}
