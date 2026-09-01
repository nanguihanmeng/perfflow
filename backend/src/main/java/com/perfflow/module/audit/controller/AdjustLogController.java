package com.perfflow.module.audit.controller;
import com.perfflow.common.api.Result;
import com.perfflow.module.audit.entity.AdjustLog;
import com.perfflow.module.audit.service.AdjustLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
// 审计日志接口（运营管理部/管理员）。
@Tag(name = "审计日志")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATION','ADMIN')")
public class AdjustLogController {

    private final AdjustLogService adjustLogService;
    @GetMapping
    @Operation(summary = "目标调整审计日志")

    // 查询列表
    public Result<List<AdjustLog>> list(@RequestParam String targetType, @RequestParam Long targetId) {

        // 返回成功响应
        return Result.ok(adjustLogService.getFullAuditLog(targetType, targetId));
    }
}
