package com.perfflow.module.assessment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.perfflow.common.api.Result;
import com.perfflow.module.assessment.dto.*;
import com.perfflow.module.assessment.service.AssessmentImportService;
import com.perfflow.module.assessment.service.AssessmentTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "考核主表")
@RestController
@RequestMapping("/assessment-tables")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AssessmentTableController {

    private final AssessmentTableService service;
    private final AssessmentImportService importService;

    @GetMapping("/{id}")
    @Operation(summary = "主表详情（含行 + 流程）")
    public Result<AssessmentTableResp> get(@PathVariable Long id) {
        return Result.ok(service.getDetail(id));
    }

    @GetMapping
    @Operation(summary = "主表分页")
    public Result<Page<AssessmentTableResp>> page(
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(service.listPage(periodId, deptId, state, pageNo, pageSize));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除主表（HR，级联删除行与流程日志）")
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @PostMapping(value = "/{id}/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入考核明细（HR，模板A1:G16格式xlsx）")
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<Void> importRows(@PathVariable Long id,
                                   @org.springframework.web.bind.annotation.RequestParam("file")
                                   org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        importService.importRows(id, file.getBytes());
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "员工提交自评（SELF_DRAFTING -> SELF_SUSPENDED）")
    public Result<Void> submit(@PathVariable Long id) {
        service.submit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/push")
    @Operation(summary = "人事确认推送（SELF_SUSPENDED -> DEPT_REVIEW）")
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<Void> push(@PathVariable Long id) {
        service.push(id);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "部门领导通过（DEPT_REVIEW -> LEAD_SCORING）")
    @PreAuthorize("hasRole('DEPT_LEAD')")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody(required = false) AssessmentRejectReq comment) {
        service.approve(id, comment == null ? null : comment.getComment());
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "部门领导打回（DEPT_REVIEW -> SELF_DRAFTING）")
    @PreAuthorize("hasRole('DEPT_LEAD')")
    public Result<Void> reject(@PathVariable Long id,
                               @Valid @RequestBody AssessmentRejectReq req) {
        service.reject(id, req.getComment());
        return Result.ok();
    }

    @PostMapping("/{id}/lead-score")
    @Operation(summary = "领导评分（LEAD_SCORING -> FINISHED；LEAD 的表由绩效委员会评分）")
    @PreAuthorize("hasAnyRole('LEAD','COMMITTEE')")
    public Result<Void> leadScore(@PathVariable Long id,
                                  @Valid @RequestBody LeadScoreReq req) {
        service.leadScore(id, req.getLeaderScore(), req.getComment());
        return Result.ok();
    }

    @PostMapping("/{id}/extend-suspend")
    @Operation(summary = "延长挂起时间（HR/ADMIN）")
    @PreAuthorize("hasAnyRole('PERFORMANCE_HR','ADMIN')")
    public Result<Void> extend(@PathVariable Long id,
                               @Valid @RequestBody ExtendSuspendReq req) {
        service.extendSuspend(id, req.getDays(), req.getReason());
        return Result.ok();
    }
}
