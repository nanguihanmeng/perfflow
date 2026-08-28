package com.perfflow.module.assessment.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.assessment.dto.FlowLogResp;
import com.perfflow.module.assessment.service.AssessmentFlowService;
import com.perfflow.module.assessment.service.AssessmentTableService;
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
@PreAuthorize("isAuthenticated()")
public class AssessmentFlowController {

    private final AssessmentFlowService flowService;
    private final AssessmentTableService tableService;

    @GetMapping
    @Operation(summary = "主表流程日志（能看该表即可看留痕）")
    public Result<List<FlowLogResp>> list(@PathVariable Long id) {
        // 复用主表数据权限：getRequired 已校验当前用户对该表可见（员工仅本人、部门领导仅本部门）
        tableService.getRequired(id);
        return Result.ok(flowService.listLogs(id));
    }
}
