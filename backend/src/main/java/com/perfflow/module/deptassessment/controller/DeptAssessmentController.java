package com.perfflow.module.deptassessment.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.deptassessment.dto.DeptActualValueReq;
import com.perfflow.module.deptassessment.dto.DeptAssessmentOptionResp;
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

    @GetMapping("/options")
    @Operation(summary = "获取本部门可填报的部门考核选项（部门绩效专员）")
    @PreAuthorize("hasRole('DEPT_STAFF')")
    public Result<List<DeptAssessmentOptionResp>> options(@RequestParam Long deptId) {
        return Result.ok(service.listOptions(deptId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询部门考核详情（各角色按数据范围）")
    @PreAuthorize("hasAnyRole('DEPT_STAFF','DEPT_LEAD','OPERATION','COMMITTEE','PERFORMANCE_HR')")
    public Result<DeptAssessmentResp> detail(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交部门考核实际完成值（部门绩效专员，→待复核）")
    @PreAuthorize("hasRole('DEPT_STAFF')")
    public Result<Void> submit(@PathVariable Long id, @Valid @RequestBody DeptActualValueReq req) {
        service.submit(id, req);
        return Result.ok();
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "复核部门考核（部门负责人，→待初审/退回）")
    @PreAuthorize("hasRole('DEPT_LEAD')")
    public Result<Void> review(@PathVariable Long id,
                               @RequestParam boolean approve,
                               @RequestParam(required = false) String comment) {
        service.review(id, approve, comment);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "初审部门考核（运营管理部，→待审批/退回整改）")
    @PreAuthorize("hasRole('OPERATION')")
    public Result<Void> audit(@PathVariable Long id,
                              @RequestParam boolean approve,
                              @RequestParam(required = false) String comment) {
        service.audit(id, approve, comment);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "最终审批部门考核（绩效委员会，→已完成）")
    @PreAuthorize("hasRole('COMMITTEE')")
    public Result<Void> approve(@PathVariable Long id) {
        service.approve(id);
        return Result.ok();
    }

    @GetMapping("/list")
    @Operation(summary = "部门考核进度列表（运营管理部/委员会/绩效管理员/部门负责人/绩效专员）")
    @PreAuthorize("hasAnyRole('DEPT_STAFF','DEPT_LEAD','OPERATION','COMMITTEE','PERFORMANCE_HR')")
    public Result<List<DeptAssessmentResp>> list() {
        return Result.ok(service.listAll());
    }
}
