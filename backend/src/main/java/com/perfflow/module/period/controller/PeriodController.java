package com.perfflow.module.period.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.period.dto.PeriodCreateReq;
import com.perfflow.module.period.dto.PeriodOpenReq;
import com.perfflow.module.period.dto.PeriodResp;
import com.perfflow.module.period.service.AssessmentPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "考核周期")
@RestController
@RequestMapping("/periods")
@RequiredArgsConstructor
public class PeriodController {

    private final AssessmentPeriodService periodService;

    @GetMapping
    @Operation(summary = "列表")
    @PreAuthorize("isAuthenticated()")
    public Result<List<PeriodResp>> list() {
        return Result.ok(periodService.list());
    }

    @GetMapping("/current")
    @Operation(summary = "当前周期")
    @PreAuthorize("isAuthenticated()")
    public Result<PeriodResp> current() {
        return Result.ok(periodService.current(java.time.LocalDate.now()));
    }

    @PostMapping
    @Operation(summary = "新建周期")
    @PreAuthorize("hasRole('HR')")
    public Result<Long> create(@Valid @RequestBody PeriodCreateReq req) {
        return Result.ok(periodService.create(req));
    }

    @PostMapping("/{id}/open")
    @Operation(summary = "开启周期（生成勾选员工考核表，不传 userIds 则为全员）")
    @PreAuthorize("hasRole('HR')")
    public Result<Void> open(@PathVariable Long id,
                             @RequestBody(required = false) PeriodOpenReq req) {
        periodService.open(id, req == null ? null : req.getUserIds());
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭周期")
    @PreAuthorize("hasRole('HR')")
    public Result<Void> close(@PathVariable Long id) {
        periodService.close(id);
        return Result.ok();
    }
}
