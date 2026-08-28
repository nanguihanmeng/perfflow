package com.perfflow.module.monitor.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.monitor.dto.ProgressResp;
import com.perfflow.module.monitor.service.ProgressService;
import com.perfflow.module.monitor.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 进度监控看板与催办接口。
 */
@Tag(name = "进度监控")
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MonitorController {

    private final ProgressService progressService;
    private final ReminderService reminderService;

    @GetMapping("/dashboard")
    @Operation(summary = "考核进度看板（运营管理部/委员会/管理员）")
    @PreAuthorize("hasAnyRole('OPERATION','COMMITTEE','ADMIN')")
    public Result<ProgressResp> dashboard() {
        return Result.ok(progressService.dashboard());
    }

    @PostMapping("/remind")
    @Operation(summary = "定向催办（运营管理部/委员会/管理员）")
    @PreAuthorize("hasAnyRole('OPERATION','COMMITTEE','ADMIN')")
    public Result<Void> remind(@RequestBody Map<String, Object> body) {
        Object raw = body.get("userIds");
        List<Long> userIds = new java.util.ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number num) {
                    userIds.add(num.longValue());
                }
            }
        }
        String content = body.get("content") == null ? "请尽快完成考核填报" : String.valueOf(body.get("content"));
        reminderService.remind(userIds, content);
        return Result.ok();
    }

    @GetMapping("/overdue-list")
    @Operation(summary = "逾期未提交人员列表（运营管理部/委员会/管理员）")
    @PreAuthorize("hasAnyRole('OPERATION','COMMITTEE','ADMIN')")
    public Result<List<Map<String, Object>>> overdueList() {
        // 简化：由前端从看板部门明细推导，此处返回空列表占位
        return Result.ok(java.util.Collections.emptyList());
    }
}
