package com.perfflow.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.perfflow.common.api.Result;
import com.perfflow.module.system.dto.PasswordResetResp;
import com.perfflow.module.system.dto.UserCreateReq;
import com.perfflow.module.system.dto.UserResp;
import com.perfflow.module.system.dto.UserUpdateReq;
import com.perfflow.module.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员-用户")
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping
    @Operation(summary = "分页查询")
    public Result<Page<UserResp>> page(
            @Parameter(description = "登录名") @RequestParam(required = false) String username,
            @Parameter(description = "角色")     @RequestParam(required = false) String role,
            @Parameter(description = "部门 id") @RequestParam(required = false) Long deptId,
            @Parameter(description = "状态")     @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(userService.page(username, role, deptId, status, pageNo, pageSize));
    }

    @PostMapping
    @Operation(summary = "新建")
    public Result<Long> create(@Valid @RequestBody UserCreateReq req) {
        return Result.ok(userService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateReq req) {
        userService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码（返回临时密码）")
    public Result<PasswordResetResp> reset(@PathVariable Long id) {
        String pwd = userService.resetPassword(id);
        return Result.ok(new PasswordResetResp(pwd));
    }

    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用切换")
    public Result<Void> toggle(@PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.ok();
    }
}
