package com.perfflow.common.excel;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.entity.DeptKpiRow;
import com.perfflow.module.deptassessment.mapper.DeptAssessmentMapper;
import com.perfflow.module.deptassessment.mapper.DeptKpiRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.List;
// Excel 模板导出服务（个人/部门填报模板）。
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelTemplateService {

    private static final String[] PERSONAL_HEADERS = {"序号", "指标类别", "指标名称", "指标分数", "工作目标", "评分标准", "完成率", "自评得分"};
    private static final String[] DEPT_HEADERS = {"部门", "行类型", "序号", "指标名称", "目标值", "评分标准", "权重(%)"};
    private static final String[] DEPT_FORMAT_NOTES = {
            "填写说明：每行一条 KPI 指标，按列对应填写",
            "· 部门：必填（批量导入用，须与系统中部门名称一致）；单部门下载模板时留空，导入时按考核自动定位",
            "· 行类型：经营业绩 / 运营指标 / 重点工作",
            "· 序号：1-3 经营业绩、4-5 运营指标、6-7 重点工作（不得重复）",
            "· 指标名称 / 目标值 / 评分标准 / 权重(%)：按实际填写，权重为 0-100 之间的数值"
    };
    private static final String MODULE_TITLE = "PerfFlow 考核填报模板";
    // dept Assessment Mapper
    private final DeptAssessmentMapper deptAssessmentMapper;
    // dept Kpi Row Mapper
    private final DeptKpiRowMapper deptKpiRowMapper;
    // 导出个人考核填报模板。

    public byte[] exportPersonalTemplate() {

        try (ExcelWriter writer = ExcelUtil.getWriter(true);

             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ExportStyleUtil style = new ExportStyleUtil(writer);
            writer.renameSheet("个人考核填报模板");
            style.writeModuleTitle(0, 0, PERSONAL_HEADERS.length, MODULE_TITLE);
            style.writeHeaderRow(1, PERSONAL_HEADERS);
            setColumnWidths(style, PERSONAL_HEADERS.length);
            writer.flush(baos);
            return baos.toByteArray();

        } catch (Exception e) {

            log.error("导出个人模板失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出个人模板失败", e);
        }
    }

    // 导出部门考核填报模板。
     // 未指定或考核不存在时仅导出表头。

    public byte[] exportDeptTemplate(Long assessmentId) {

        try (ExcelWriter writer = ExcelUtil.getWriter(true);

             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ExportStyleUtil style = new ExportStyleUtil(writer);
            writer.renameSheet("部门考核填报模板");
            style.writeModuleTitle(0, 0, DEPT_HEADERS.length - 1, MODULE_TITLE);
            // 格式说明行（小字灰色）
            int noteRow = 1;

            for (String note : DEPT_FORMAT_NOTES) {

                style.writeNote(noteRow++, 0, note);
            }

            // 表头行
            style.writeHeaderRow(noteRow, DEPT_HEADERS);
            int rowIdx = noteRow + 1;
            List<DeptKpiRow> rows = loadKpiRows(assessmentId);

            for (DeptKpiRow r : rows) {

                style.writeDataRow(rowIdx++, new Object[]{

                        "", r.getRowType(), r.getSeqNo(), r.getIndicatorName(),
                        r.getTargetValue(), r.getScoringStandard(), r.getWeight()
                });
            }

            setColumnWidths(style, DEPT_HEADERS.length);
            writer.flush(baos);
            return baos.toByteArray();

        } catch (Exception e) {

            log.error("导出部门模板失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出部门模板失败", e);
        }
    }

    private List<DeptKpiRow> loadKpiRows(Long assessmentId) {

        // 判空处理
        if (assessmentId == null) {

            return List.of();
        }

        // 查询单条
        DeptAssessment assessment = deptAssessmentMapper.selectById(assessmentId);

        // 判空处理
        if (assessment == null) {

            return List.of();
        }

        // 查询列表
        return deptKpiRowMapper.selectList(new QueryWrapper<DeptKpiRow>()
                .eq("dept_assessment_id", assessmentId)
                .orderByAsc("seq_no"));
    }

    private void setColumnWidths(ExportStyleUtil style, int colCount) {

        for (int i = 0; i < colCount; i++) {

            style.setColumnWidth(i, 20);
        }
    }
}
