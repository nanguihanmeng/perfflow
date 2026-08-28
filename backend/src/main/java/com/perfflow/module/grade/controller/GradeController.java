package com.perfflow.module.grade.controller;

import com.perfflow.common.api.Result;
import com.perfflow.module.grade.dto.GradeQuotaReq;
import com.perfflow.module.grade.dto.WeightConfigReq;
import com.perfflow.module.grade.entity.GradeQuotaConfig;
import com.perfflow.module.grade.entity.WeightConfig;
import com.perfflow.module.grade.service.GradeCalculationService;
import com.perfflow.module.grade.service.GradeQuotaService;
import com.perfflow.module.grade.service.WeightConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 等级配额与权重配置接口。
 */
@Tag(name = "等级配额与权重")
@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GradeController {

    private final GradeQuotaService quotaService;
    private final WeightConfigService weightService;
    private final GradeCalculationService calculationService;

    @GetMapping("/grade/quota-config")
    @Operation(summary = "等级配额配置列表（运营管理部/管理员）")
    @PreAuthorize("hasAnyRole('OPERATION','ADMIN')")
    public Result<List<GradeQuotaConfig>> quotaList() {
        return Result.ok(quotaService.list());
    }

    @PostMapping("/grade/quota-config")
    @Operation(summary = "新增等级配额配置")
    @PreAuthorize("hasAnyRole('OPERATION','ADMIN')")
    public Result<Long> quotaCreate(@Valid @RequestBody GradeQuotaReq req) {
        return Result.ok(quotaService.create(req));
    }

    @PutMapping("/grade/quota-config/{id}")
    @Operation(summary = "更新等级配额配置")
    @PreAuthorize("hasAnyRole('OPERATION','ADMIN')")
    public Result<Void> quotaUpdate(@PathVariable Long id, @Valid @RequestBody GradeQuotaReq req) {
        quotaService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/grade/quota-config/{id}")
    @Operation(summary = "删除等级配额配置")
    @PreAuthorize("hasAnyRole('OPERATION','ADMIN')")
    public Result<Void> quotaDelete(@PathVariable Long id) {
        quotaService.delete(id);
        return Result.ok();
    }

    @PostMapping("/grade/auto-calculate")
    @Operation(summary = "触发等级自动计算（运营管理部）")
    @PreAuthorize("hasRole('OPERATION')")
    public Result<Void> autoCalculate(@RequestParam Long periodId) {
        calculationService.autoCalculate(periodId);
        return Result.ok();
    }

    @GetMapping("/weight/config")
    @Operation(summary = "权重配置列表（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<WeightConfig>> weightList() {
        return Result.ok(weightService.list());
    }

    @PostMapping("/weight/config")
    @Operation(summary = "新增权重配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> weightCreate(@Valid @RequestBody WeightConfigReq req) {
        return Result.ok(weightService.create(req));
    }

    @PutMapping("/weight/config/{id}")
    @Operation(summary = "更新权重配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> weightUpdate(@PathVariable Long id, @Valid @RequestBody WeightConfigReq req) {
        weightService.update(id, req);
        return Result.ok();
    }
}
