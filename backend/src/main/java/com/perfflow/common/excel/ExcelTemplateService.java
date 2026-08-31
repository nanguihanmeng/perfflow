package com.perfflow.common.excel;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Excel 模板导出服务（个人/部门填报模板）。
 */
@Slf4j
@Service
public class ExcelTemplateService {

    /** 个人考核填报模板列头 */
    private static final String[] PERSONAL_HEADERS = {"序号", "指标类别", "指标名称", "指标分数", "工作目标", "评分标准", "完成率", "自评得分"};
    /** 部门考核填报模板列头（得分由系统按完成率自动计算，实际完成值由绩效专员填报，均不在 HR 模板中维护） */
    private static final String[] DEPT_HEADERS = {"行类型", "序号", "指标名称", "目标值", "评分标准", "权重(%)"};

    /**
     * 导出个人考核填报模板。
     *
     * @return xlsx 字节
     */
    public byte[] exportPersonalTemplate() {
        return exportTemplate("个人考核填报模板", PERSONAL_HEADERS);
    }

    /**
     * 导出部门考核填报模板。
     *
     * @return xlsx 字节
     */
    public byte[] exportDeptTemplate() {
        return exportTemplate("部门考核填报模板", DEPT_HEADERS);
    }

    private byte[] exportTemplate(String sheetName, String[] headers) {
        try (ExcelWriter writer = ExcelUtil.getWriter();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            writer.renameSheet(sheetName);
            writer.writeRow(headers, true);
            // 列宽
            for (int i = 0; i < headers.length; i++) {
                writer.setColumnWidth(i, 20);
            }
            writer.getStyleSet().setWrapText();
            writer.flush(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("导出模板失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出模板失败", e);
        }
    }
}
