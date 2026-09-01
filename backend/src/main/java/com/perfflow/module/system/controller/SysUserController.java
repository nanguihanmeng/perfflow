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

    // 业务处理
    public Result<Page<UserResp>> page(
            @Parameter(description = "登录名") @RequestParam(required = false) String username,
            @Parameter(description = "角色")     @RequestParam(required = false) String role,
            @Parameter(description = "部门 id") @RequestParam(required = false) Long deptId,
            @Parameter(description = "状态")     @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {

        // 返回成功响应
        return Result.ok(userService.page(username, role, deptId, status, pageNo, pageSize));
    }

    @PostMapping
    @Operation(summary = "新建")

    // 新增数据
    public Result<Long> create(@Valid @RequestBody UserCreateReq req) {

        // 返回成功响应
        return Result.ok(userService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改")

    // 更新数据
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateReq req) {

        // 更新记录
        userService.update(id, req);
        // 返回成功响应
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除")

    // 删除数据
    public Result<Void> delete(@PathVariable Long id) {

        // 调用业务服务
        userService.delete(id);
        // 返回成功响应
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码（返回临时密码）")

    // 业务处理
    public Result<PasswordResetResp> reset(@PathVariable Long id) {

        // 调用业务服务
        String pwd = userService.resetPassword(id);
        // 返回成功响应
        return Result.ok(new PasswordResetResp(pwd));
    }

    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用切换")

    // 业务处理
    public Result<Void> toggle(@PathVariable Long id) {

        // 调用业务服务
        userService.toggleStatus(id);
        // 返回成功响应
        return Result.ok();
    }
}
