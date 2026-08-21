package com.perfflow.module.assessment.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.assessment.dto.FlowLogResp;
import com.perfflow.module.assessment.service.AssessmentFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "流程日志")
@RestController
@RequestMapping("/assessment-tables/{id}/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PERFORMANCE_HR','LEAD','DEPT_LEAD')")
public class AssessmentFlowController {

    private final AssessmentFlowService flowService;

    @GetMapping
    @Operation(summary = "主表流程日志")
    public Result<List<FlowLogResp>> list(@PathVariable Long id) {
        // 注意：日志接口不在 admin 业务隔离范围（HR/LEAD/DEPT_LEAD 才有资格看）
        return Result.ok(flowService.listLogs(id));
    }
}
