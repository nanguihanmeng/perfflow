package com.perfflow.module.deptassessment.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.deptassessment.dto.DeptAssessmentReq;
import com.perfflow.module.deptassessment.dto.DeptAssessmentResp;
import com.perfflow.module.deptassessment.service.DeptAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门考核接口。
 */
@Tag(name = "部门考核")
@RestController
@RequestMapping("/dept-assessments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DeptAssessmentController {

    private final DeptAssessmentService service;

    @GetMapping("/current")
    @Operation(summary = "获取本部门当前周期考核表（部门绩效专员）")
    @PreAuthorize("hasRole('DEPT_STAFF')")
    public Result<DeptAssessmentResp> current(@RequestParam Long deptId) {
        return Result.ok(service.current(deptId));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交部门考核（部门绩效专员，→待复核）")
    @PreAuthorize("hasRole('DEPT_STAFF')")
    public Result<Void> submit(@RequestParam Long deptId, @Valid @RequestBody DeptAssessmentReq req) {
        service.submit(deptId, req);
        return Result.ok();
    }

    @PostMapping("/{deptId}/review")
    @Operation(summary = "复核部门考核（部门负责人，→待初审/退回）")
    @PreAuthorize("hasRole('DEPT_LEAD')")
    public Result<Void> review(@PathVariable Long deptId,
                               @RequestParam boolean approve,
                               @RequestParam(required = false) String comment) {
        service.review(deptId, approve, comment);
        return Result.ok();
    }

    @PostMapping("/{deptId}/audit")
    @Operation(summary = "初审部门考核（运营管理部，→待审批/退回整改）")
    @PreAuthorize("hasRole('OPERATION')")
    public Result<Void> audit(@PathVariable Long deptId,
                              @RequestParam boolean approve,
                              @RequestParam(required = false) String comment) {
        service.audit(deptId, approve, comment);
        return Result.ok();
    }

    @PostMapping("/{deptId}/approve")
    @Operation(summary = "最终审批部门考核（绩效委员会，→已完成）")
    @PreAuthorize("hasRole('COMMITTEE')")
    public Result<Void> approve(@PathVariable Long deptId) {
        service.approve(deptId);
        return Result.ok();
    }

    @GetMapping("/list")
    @Operation(summary = "部门考核进度列表（运营管理部/委员会/管理员）")
    @PreAuthorize("hasAnyRole('OPERATION','COMMITTEE','ADMIN')")
    public Result<List<DeptAssessmentResp>> list() {
        return Result.ok(service.listAll());
    }
}
