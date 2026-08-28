package com.perfflow.common.excel;

import com.perfflow.common.api.Result;
import com.perfflow.common.excel.ExcelImportService;
import com.perfflow.common.excel.ExcelTemplateService;
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

/**
 * Excel 模板下载与数据导入接口。
 */
@Tag(name = "Excel 导入导出")
@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExcelController {

    private final ExcelTemplateService templateService;
    private final ExcelImportService importService;

    @GetMapping("/export/template/personal")
    @Operation(summary = "下载个人考核填报模板")
    public ResponseEntity<byte[]> personalTemplate() {
        return download(templateService.exportPersonalTemplate(), "personal-template.xls");
    }

    @GetMapping("/export/template/dept")
    @Operation(summary = "下载部门考核填报模板")
    public ResponseEntity<byte[]> deptTemplate() {
        return download(templateService.exportDeptTemplate(), "dept-template.xls");
    }

    @PostMapping("/import/personal")
    @Operation(summary = "导入个人考核数据")
    public Result<Void> importPersonal(@RequestParam Long tableId,
                                       @RequestParam("file") MultipartFile file) throws IOException {
        importService.importPersonal(tableId, file.getBytes());
        return Result.ok();
    }

    @PostMapping("/import/dept")
    @Operation(summary = "导入部门考核数据")
    public Result<Void> importDept(@RequestParam Long assessmentId,
                                   @RequestParam("file") MultipartFile file) throws IOException {
        importService.importDept(assessmentId, file.getBytes());
        return Result.ok();
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String filename) {
        String encoded = new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
