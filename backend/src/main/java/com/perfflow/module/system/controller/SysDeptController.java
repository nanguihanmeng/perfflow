package com.perfflow.module.system.controller;
import com.perfflow.common.api.Result;
import com.perfflow.module.system.dto.DeptReq;
import com.perfflow.module.system.dto.DeptResp;
import com.perfflow.module.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "管理员-部门")
@RestController
@RequestMapping("/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;
    @GetMapping
    @Operation(summary = "部门列表")

    // 查询列表
    public Result<List<DeptResp>> list() { return Result.ok(deptService.list()); }

    @PostMapping
    @Operation(summary = "新建部门")

    // 新增数据
    public Result<Long> create(@Valid @RequestBody DeptReq req) { return Result.ok(deptService.create(req)); }

    @PutMapping("/{id}")
    @Operation(summary = "修改部门")

    // 更新数据
    public Result<Void> update(@PathVariable Long id, @RequestBody DeptReq req) {

        // 更新记录
        deptService.update(id, req);
        // 返回成功响应
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门")

    // 删除数据
    public Result<Void> delete(@PathVariable Long id) {

        // 调用业务服务
        deptService.delete(id);
        // 返回成功响应
        return Result.ok();
    }
}
