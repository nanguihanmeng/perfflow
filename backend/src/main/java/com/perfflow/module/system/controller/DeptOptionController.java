package com.perfflow.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.Result;
import com.perfflow.module.system.dto.DeptResp;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门选项（HR 开启部门线周期勾选用）。
 */
@Tag(name = "部门选项")
@RestController
@RequestMapping("/departments/options")
@PreAuthorize("hasRole('PERFORMANCE_HR')")
@RequiredArgsConstructor
public class DeptOptionController {

    private final SysDepartmentMapper deptMapper;

    @GetMapping
    @Operation(summary = "部门下拉列表")
    public Result<List<DeptResp>> list() {
        List<SysDepartment> all = deptMapper.selectList(
                new QueryWrapper<SysDepartment>().orderByAsc("sort").orderByAsc("id"));
        List<DeptResp> out = new ArrayList<>(all.size());
        for (SysDepartment d : all) {
            out.add(DeptResp.from(d));
        }
        return Result.ok(out);
    }
}
