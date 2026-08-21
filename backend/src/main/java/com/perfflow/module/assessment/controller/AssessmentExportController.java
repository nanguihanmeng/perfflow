package com.perfflow.module.assessment.controller;

import com.perfflow.module.assessment.service.AssessmentExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;

@Tag(name = "考核导出")
@RestController
@RequestMapping("/assessment-tables/export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PERFORMANCE_HR')")
public class AssessmentExportController {

    private final AssessmentExportService exportService;

    @GetMapping
    @Operation(summary = "导出 Excel（按周期）")
    public void excel(@RequestParam(required = false) Long periodId,
                      HttpServletResponse resp) throws IOException {
        byte[] bytes = exportService.exportExcel(periodId);
        resp.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        resp.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=assessment_" + (periodId == null ? "all" : periodId) + ".xlsx");
        try (OutputStream os = resp.getOutputStream()) {
            os.write(bytes);
        }
    }

    @GetMapping("/print")
    @Operation(summary = "打印 HTML 视图")
    public void print(@RequestParam(required = false) Long periodId,
                      HttpServletResponse resp) throws IOException {
        String html = exportService.exportPrintHtml(periodId);
        resp.setContentType(MediaType.TEXT_HTML_VALUE);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(html);
    }
}
