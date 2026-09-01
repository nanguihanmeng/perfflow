package com.perfflow.common.excel;
import com.perfflow.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
//
 // Excel 模板下载与数据导入接口。
 //
@Tag(name = "Excel 导入导出")
@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExcelController {

    private final ExcelTemplateService templateService;
    private final ExcelImportService importService;
    //
     // 下载个人考核填报模板。
     //
    @GetMapping("/export/template/personal")
    @Operation(summary = "下载个人考核填报模板")
    public ResponseEntity<byte[]> personalTemplate() {

        return download(templateService.exportPersonalTemplate(), "personal-template.xlsx");
    }

    //
     // 下载部门考核填报模板，可带 assessmentId 预填现有 KPI。
     //
    @GetMapping("/export/template/dept")
    @Operation(summary = "下载部门考核填报模板（可带 assessmentId 预填现有 KPI）")
    public ResponseEntity<byte[]> deptTemplate(@RequestParam(required = false) Long assessmentId) {

        return download(templateService.exportDeptTemplate(assessmentId), "dept-template.xlsx");
    }

    //

     //
    @PostMapping("/import/personal")
    @Operation(summary = "导入个人考核数据")
    public Result<Void> importPersonal(@RequestParam Long tableId,
                                       @RequestParam("file") MultipartFile file) throws IOException {

        // 读取上传文件字节
        importService.importPersonal(tableId, file.getBytes());
        // 返回成功响应
        return Result.ok();
    }

    //

     //
    @PostMapping("/import/dept")
    @Operation(summary = "导入部门考核指标（绩效考核管理员）")
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<Void> importDept(@RequestParam Long assessmentId,
                                   @RequestParam("file") MultipartFile file) throws IOException {

        // 读取上传文件字节
        importService.importDept(assessmentId, file.getBytes());
        // 返回成功响应
        return Result.ok();
    }

    //
     // 按周期批量导入部门考核指标（绩效考核管理员）。
     //
    @PostMapping("/import/dept/batch")
    @Operation(summary = "按周期批量导入部门考核指标（绩效考核管理员）")
    @PreAuthorize("hasRole('PERFORMANCE_HR')")
    public Result<Void> importDeptBatch(@RequestParam Long periodId,
                                        @RequestParam("file") MultipartFile file) throws IOException {

        // 读取上传文件字节
        importService.importDeptBatch(periodId, file.getBytes());
        // 返回成功响应
        return Result.ok();
    }

    //
     // 构造文件下载响应。
     //
    private ResponseEntity<byte[]> download(byte[] bytes, String filename) {

        String encoded = new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
