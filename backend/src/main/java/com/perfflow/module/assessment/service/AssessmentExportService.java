package com.perfflow.module.assessment.service;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.dto.AssessmentTableResp;
import com.perfflow.module.assessment.dto.RowResp;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.mapper.AssessmentRowMapper;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考核结果导出（仅 HR）。
 */
@Service
@RequiredArgsConstructor
public class AssessmentExportService {

    private final AssessmentTableMapper tableMapper;
    private final AssessmentRowMapper rowMapper;
    private final AssessmentPeriodMapper periodMapper;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final AssessmentTableService tableService;

    /**
     * 导出某周期所有主表为 Excel（模板 A1:G16 格式）。
     *
     * @return byte[]
     */
    public byte[] exportExcel(Long periodId) {
        AssessmentPeriod period = periodId == null ? null : periodMapper.selectById(periodId);
        List<AssessmentTable> tables = tableMapper.selectList(
                new QueryWrapper<AssessmentTable>()
                        .eq(periodId != null, "period_id", periodId)
                        .orderByAsc("dept_id").orderByAsc("user_id"));

        // 批量加载用户/部门缓存，避免 N+1
        Map<Long, String> userCache = new HashMap<>();
        Map<Long, String> deptCache = new HashMap<>();
        Map<Long, String> deptLeadCache = new HashMap<>();
        for (AssessmentTable t : tables) {
            if (t.getUserId() != null && !userCache.containsKey(t.getUserId())) {
                SysUser u = userMapper.selectById(t.getUserId());
                userCache.put(t.getUserId(), u == null ? "" : u.getRealName());
            }
            if (t.getDeptId() != null && !deptCache.containsKey(t.getDeptId())) {
                SysDepartment d = deptMapper.selectById(t.getDeptId());
                deptCache.put(t.getDeptId(), d == null ? "" : d.getName());
                SysUser lead = userMapper.selectOne(new QueryWrapper<SysUser>()
                        .eq("dept_id", t.getDeptId()).eq("role", "DEPT_LEAD")
                        .eq("status", 1).last("LIMIT 1"));
                deptLeadCache.put(t.getDeptId(), lead == null ? "" : lead.getRealName());
            }
        }

        try (ExcelWriter writer = ExcelUtil.getWriter();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 列宽：文本列加宽 + 自动换行，确保格子内文字完整显示
            writer.setColumnWidth(0, 18);   // A 部门/标题
            writer.setColumnWidth(1, 14);   // B 序号/被考核人
            writer.setColumnWidth(2, 14);   // C 指标类别/岗位
            writer.setColumnWidth(3, 30);   // D 指标名称
            writer.setColumnWidth(4, 12);   // E 指标分数
            writer.setColumnWidth(5, 36);   // F 工作目标
            writer.setColumnWidth(6, 36);   // G 评分标准
            writer.setColumnWidth(7, 12);   // H 完成率
            writer.setColumnWidth(8, 12);   // I 自评得分
            writer.getStyleSet().setWrapText();
            int idx = 0;
            for (AssessmentTable t : tables) {
                writeTable(writer, t, period, idx, userCache, deptCache, deptLeadCache);
                idx += 20; // 每表占 16 行 + 4 空行
            }
            writer.flush(baos);
            return baos.toByteArray();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    /** 单表按模板 A1:G16 布局写入（标题/信息栏/表头/10行/总分/签字栏） */
    private void writeTable(ExcelWriter writer, AssessmentTable t, AssessmentPeriod period, int startRow,
                            Map<Long, String> userCache, Map<Long, String> deptCache,
                            Map<Long, String> deptLeadCache) {
        String realName = userCache.getOrDefault(t.getUserId(), "");
        String deptName = deptCache.getOrDefault(t.getDeptId(), "");
        String deptLeadName = deptLeadCache.getOrDefault(t.getDeptId(), "");
        String position = t.getPosition() == null ? "" : t.getPosition();
        String periodText = period == null ? "" : period.getName();

        // 第1行 标题（A1:I1 合并）
        writeRow(writer, new Object[]{"岗位季度绩效考核表"}, startRow);
        // 第2行 信息栏
        writeRow(writer, new Object[]{
                "部门：" + deptName, "被考核人：" + realName, "岗位：" + position,
                "部门负责人：" + deptLeadName
        }, startRow + 1);
        // 第3行 考核期
        writeRow(writer, new Object[]{"考核期：" + periodText, "填表日期："}, startRow + 2);
        // 第4行 表头
        writeRow(writer, new Object[]{"", "序号", "指标类别", "指标名称", "指标分数", "工作目标", "评分标准", "完成率", "自评得分"}, startRow + 3);
        // 第5-14行 数据
        List<AssessmentRow> rows = rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));
        for (int i = 0; i < rows.size(); i++) {
            AssessmentRow r = rows.get(i);
            RowResp rr = tableService.toRowResp(r, false);
            writeRow(writer, new Object[]{
                    "", r.getSeq(), rr.getCategory(), nullSafe(rr.getIndicatorName()), rr.getBaseScore(),
                    nullSafe(rr.getWorkTarget()), nullSafe(rr.getScoreCriteria()),
                    rr.getCompletionRate() == null ? "" : rr.getCompletionRate() + "%",
                    rr.getSelfScore() == null ? "" : rr.getSelfScore()
            }, startRow + 4 + i);
        }
        // 第15行 总分
        AssessmentTableResp hdr = tableService.toTableResp(t, false);
        writeRow(writer, new Object[]{
                "总分", "", "", "", hdr.getSelfTotalScore() == null ? "" : hdr.getSelfTotalScore(),
                "最终得分", hdr.getFinalScore() == null ? "" : hdr.getFinalScore(),
                "考核结果", hdr.getGrade() == null ? "" : hdr.getGrade()
        }, startRow + 14);
        // 第16行 签字栏
        writeRow(writer, new Object[]{
                "部门负责人签字确认：", "被考核人签字确认：", "日期："
        }, startRow + 15);
    }

    /** 打印 HTML（模板 A1:G16 布局） */
    public String exportPrintHtml(Long periodId) {
        AssessmentPeriod period = periodId == null ? null : periodMapper.selectById(periodId);
        List<AssessmentTable> tables = tableMapper.selectList(
                new QueryWrapper<AssessmentTable>()
                        .eq(periodId != null, "period_id", periodId)
                        .orderByAsc("dept_id").orderByAsc("user_id"));
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='utf-8'><title>")
          .append(period == null ? "考核汇总" : period.getName()).append("</title>")
          .append("<style>")
          .append("body{font-family:Arial,sans-serif;}table{border-collapse:collapse;width:100%;margin-bottom:20px;}")
          .append("th,td{border:1px solid #000;padding:6px 10px;}th{background:#f4f4f4;}h2{margin:8px 0;}")
          .append(".title{font-size:18px;font-weight:bold;text-align:center;margin:8px 0;}")
          .append(".info{font-size:13px;margin:4px 0;}")
          .append("@media print { .no-print{display:none;} }")
          .append("</style></head><body>");
        sb.append("<div class='no-print'><button onclick='window.print()'>打印</button></div>");
        sb.append("<h1>").append(period == null ? "考核汇总" : period.getName()).append("</h1>");

        for (AssessmentTable t : tables) {
            SysUser u = userMapper.selectById(t.getUserId());
            SysDepartment d = deptMapper.selectById(t.getDeptId());
            SysUser deptLead = d == null ? null : userMapper.selectOne(new QueryWrapper<SysUser>()
                    .eq("dept_id", t.getDeptId()).eq("role", "DEPT_LEAD")
                    .eq("status", 1).last("LIMIT 1"));
            String realName = u == null ? "" : u.getRealName();
            String deptName = d == null ? "" : d.getName();
            String deptLeadName = deptLead == null ? "" : deptLead.getRealName();
            String position = t.getPosition() == null ? "" : t.getPosition();

            sb.append("<div class='title'>岗位季度绩效考核表</div>");
            sb.append("<div class='info'>部门：").append(deptName)
              .append("　被考核人：").append(realName)
              .append("　岗位：").append(position)
              .append("　部门负责人：").append(deptLeadName).append("</div>");
            sb.append("<div class='info'>考核期：").append(period == null ? "" : period.getName())
              .append("　填表日期：</div>");
            sb.append("<table><thead><tr><th>序号</th><th>指标类别</th><th>指标名称</th><th>指标分数</th>")
              .append("<th>工作目标</th><th>评分标准</th><th>完成率</th><th>自评得分</th></tr></thead><tbody>");
            List<AssessmentRow> rows = rowMapper.selectList(
                    new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));
            for (AssessmentRow r : rows) {
                RowResp rr = tableService.toRowResp(r, false);
                sb.append("<tr><td>").append(r.getSeq()).append("</td>")
                  .append("<td>").append(r.getCategory()).append("</td>")
                  .append("<td>").append(nullSafe(rr.getIndicatorName())).append("</td>")
                  .append("<td>").append(rr.getBaseScore()).append("</td>")
                  .append("<td>").append(nullSafe(rr.getWorkTarget())).append("</td>")
                  .append("<td>").append(nullSafe(rr.getScoreCriteria())).append("</td>")
                  .append("<td>").append(rr.getCompletionRate() == null ? "" : rr.getCompletionRate() + "%").append("</td>")
                  .append("<td>").append(rr.getSelfScore() == null ? "" : rr.getSelfScore()).append("</td>")
                  .append("</tr>");
            }
            AssessmentTableResp hdr = tableService.toTableResp(t, false);
            sb.append("<tr><td colspan='4'>总分</td><td>")
              .append(hdr.getSelfTotalScore() == null ? "" : hdr.getSelfTotalScore()).append("</td>")
              .append("<td>最终得分</td><td>").append(hdr.getFinalScore() == null ? "" : hdr.getFinalScore())
              .append("</td><td>考核结果</td><td>").append(hdr.getGrade() == null ? "" : hdr.getGrade())
              .append("</td></tr>");
            sb.append("</tbody></table>");
            sb.append("<div class='info'>部门负责人签字确认：　　　　被考核人签字确认：　　　　日期：</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private String nullSafe(Object o) { return o == null ? "" : o.toString().replace("\n", "<br/>"); }

    /** 把一行数组按列写入指定行（writeCellValue(x=列, y=行)） */
    private static void writeRow(ExcelWriter writer, Object[] row, int rowIdx) {
        for (int col = 0; col < row.length; col++) {
            writer.writeCellValue(col, rowIdx, row[col]);
        }
    }
}
