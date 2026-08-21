package com.perfflow.module.period.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.period.dto.PeriodCreateReq;
import com.perfflow.module.period.dto.PeriodOpenReq;
import com.perfflow.module.period.dto.PeriodResp;
import com.perfflow.module.period.service.AssessmentPeriodService;
import com.perfflow.module.period.service.PeriodImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Tag(name = "考核周期")
@RestController
@RequestMapping("/periods")
@RequiredArgsConstructor
public class PeriodController {

    private final AssessmentPeriodService periodService;
    private final PeriodImportService periodImportService;

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
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<Long> create(@Valid @RequestBody PeriodCreateReq req) {
        return Result.ok(periodService.create(req));
    }

    @PostMapping("/{id}/open")
    @Operation(summary = "开启周期（生成勾选员工考核表，不传 userIds 则为全员）")
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<Void> open(@PathVariable Long id,
                             @RequestBody(required = false) PeriodOpenReq req) {
        periodService.open(id, req == null ? null : req.getUserIds());
        return Result.ok();
    }

    @PostMapping(value = "/{id}/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入周期考核明细（员工+明细一个文件，返回参与员工ID）")
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<List<Long>> importPeriod(@PathVariable Long id,
                                           @org.springframework.web.bind.annotation.RequestParam("file")
                                           org.springframework.web.multipart.MultipartFile file) throws IOException {
        var period = periodService.required(id);
        List<Long> userIds = periodImportService.importPeriod(period, file.getBytes());
        return Result.ok(userIds);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭周期")
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<Void> close(@PathVariable Long id) {
        periodService.close(id);
        return Result.ok();
    }
}
