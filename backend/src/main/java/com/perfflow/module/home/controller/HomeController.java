package com.perfflow.module.home.controller;
import com.perfflow.common.api.Result;
import com.perfflow.module.home.dto.RemindersResp;
import com.perfflow.module.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "首页提醒")
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class HomeController {

    private final HomeService homeService;
    @GetMapping("/reminders")
    @Operation(summary = "首页提醒")
    // 催办ers。

    // 查询首页提醒
    public Result<RemindersResp> reminders() {

        // 返回成功响应
        return Result.ok(homeService.load());
    }
}
