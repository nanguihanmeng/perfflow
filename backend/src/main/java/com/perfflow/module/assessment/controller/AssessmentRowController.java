package com.perfflow.module.assessment.controller;
import com.perfflow.common.api.Result;
import com.perfflow.module.assessment.dto.RowReq;
import com.perfflow.module.assessment.service.AssessmentRowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@Tag(name = "考核行")
@RestController
@RequestMapping("/assessment-tables/{tableId}/rows")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AssessmentRowController {

    private final AssessmentRowService rowService;
    @PutMapping("/{rowId}")
    @Operation(summary = "更新行完成率（EMP 自评阶段 / DEPT_LEAD 部门审核阶段）")
    public Result<Void> update(@PathVariable Long tableId,
                               @PathVariable Long rowId,
                               @Valid @RequestBody RowReq req) {

        // 更新记录
        rowService.update(tableId, rowId, req);
        // 返回成功响应
        return Result.ok();
    }

    @PutMapping("/position")
    @Operation(summary = "更新主表岗位（被考核人信息栏）")

    // 更新数据
    public Result<Void> updatePosition(@PathVariable Long tableId,
                                       @RequestBody RowReq req) {

        // 调用业务服务
        rowService.updatePosition(tableId, req.getPosition());
        // 返回成功响应
        return Result.ok();
    }
}
