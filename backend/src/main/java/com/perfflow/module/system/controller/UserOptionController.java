package com.perfflow.module.system.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.system.dto.UserResp;
import com.perfflow.module.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工选项（HR 开启个人线周期勾选用）。
 */
@Tag(name = "员工选项")
@RestController
@RequestMapping("/users/options")
@PreAuthorize("hasRole('PERFORMANCE_HR')")
@RequiredArgsConstructor
public class UserOptionController {

    private final SysUserService userService;

    @GetMapping
    @Operation(summary = "参与考核的用户列表（可按角色过滤）")
    public Result<List<UserResp>> list(@RequestParam(required = false) List<String> roles) {
        return Result.ok(userService.listOptions(roles));
    }
}
