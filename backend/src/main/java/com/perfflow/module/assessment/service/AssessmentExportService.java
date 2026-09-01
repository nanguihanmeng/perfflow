package com.perfflow.module.assessment.service;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.common.excel.ExportStyleUtil;
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
// 考核结果导出（仅 HR）。
@Service
@RequiredArgsConstructor
public class AssessmentExportService {

    private final AssessmentTableMapper tableMapper;
    private final AssessmentRowMapper rowMapper;
    private final AssessmentPeriodMapper periodMapper;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final AssessmentTableService tableService;
    // 导出某周期所有主表为 Excel（模板 A1:G16 格式）。

    // 导出考核 Excel
    public byte[] exportExcel(Long periodId) {

        // 查询单条
        AssessmentPeriod period = periodId == null ? null : periodMapper.selectById(periodId);
        // 查询列表
        List<AssessmentTable> tables = tableMapper.selectList(
                new QueryWrapper<AssessmentTable>()
                        .eq(periodId != null, "period_id", periodId)
                        .orderByAsc("dept_id").orderByAsc("user_id"));
        // 批量加载用户/部门缓存，避免 N+1
        Map<Long, String> userCache = new HashMap<>();
        // 构建集合容器
        Map<Long, String> deptCache = new HashMap<>();
        // 构建集合容器
        Map<Long, String> deptLeadCache = new HashMap<>();

        for (AssessmentTable t : tables) {

            // 非空才处理
            if (t.getUserId() != null && !userCache.containsKey(t.getUserId())) {

                // 查询单条
                SysUser u = userMapper.selectById(t.getUserId());
                userCache.put(t.getUserId(), u == null ? "" : u.getRealName());
            }

            // 非空才处理
            if (t.getDeptId() != null && !deptCache.containsKey(t.getDeptId())) {

                // 查询单条
                SysDepartment d = deptMapper.selectById(t.getDeptId());
                deptCache.put(t.getDeptId(), d == null ? "" : d.getName());
                // 查询单条
                SysUser lead = userMapper.selectOne(new QueryWrapper<SysUser>()
                        .eq("dept_id", t.getDeptId()).eq("role", "DEPT_LEAD")
                        .eq("status", 1).last("LIMIT 1"));
                deptLeadCache.put(t.getDeptId(), lead == null ? "" : lead.getRealName());
            }
        }

        try (ExcelWriter writer = ExcelUtil.getWriter();

             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ExportStyleUtil style = new ExportStyleUtil(writer);
            setColumnWidths(style);
            int idx = 0;

            for (AssessmentTable t : tables) {

                writeTable(writer, style, t, period, idx, userCache, deptCache, deptLeadCache);
                idx += 20; // 每表占 16 行 + 4 空行
            }

            writer.flush(baos);
            return baos.toByteArray();

        } catch (BizException e) {

            throw e;

        } catch (Exception e) {

            // 校验失败抛异常
            throw new BizException(ResultCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private void writeTable(ExcelWriter writer, ExportStyleUtil style, AssessmentTable t,
                            AssessmentPeriod period, int startRow,
                            // 构建集合容器
                            Map<Long, String> userCache, Map<Long, String> deptCache,

                            // 构建集合容器
                            Map<Long, String> deptLeadCache) {

        String realName = userCache.getOrDefault(t.getUserId(), "");
        String deptName = deptCache.getOrDefault(t.getDeptId(), "");
        String deptLeadName = deptLeadCache.getOrDefault(t.getDeptId(), "");
        String position = t.getPosition() == null ? "" : t.getPosition();
        String periodText = period == null ? "" : period.getName();
        // 装饰条（最左列，覆盖整表 16 行）
        style.paintBand(startRow, startRow + 15, 0);
        // 第1行 标题（A1:I1 合并，一级模块标题样式）
        style.writeModuleTitle(startRow, 1, 8, "岗位季度绩效考核表");
        // 第2行 信息栏
        style.writeDataRow(startRow + 1, new Object[]{

                "部门：" + deptName, "被考核人：" + realName, "岗位：" + position,
                "部门负责人：" + deptLeadName, "", "", "", "", ""
        });
        // 第3行 考核期
        style.writeDataRow(startRow + 2, new Object[]{

                "考核期：" + periodText, "填表日期：", "", "", "", "", "", "", ""
        });
        // 第4行 表头（二级表头样式）
        style.writeHeaderRow(startRow + 3, new Object[]{

                "", "序号", "指标类别", "指标名称", "指标分数", "工作目标", "评分标准", "完成率", "自评得分"
        });
        // 第5-14行 数据
        List<AssessmentRow> rows = rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));

        for (int i = 0; i < rows.size(); i++) {

            AssessmentRow r = rows.get(i);
            // 调用业务服务
            RowResp rr = tableService.toRowResp(r, false);

            style.writeDataRow(startRow + 4 + i, new Object[]{

                    "", r.getSeq(), rr.getCategory(), nullSafe(rr.getIndicatorName()), rr.getBaseScore(),
                    nullSafe(rr.getWorkTarget()), nullSafe(rr.getScoreCriteria()),
                    rr.getCompletionRate() == null ? "" : rr.getCompletionRate() + "%",
                    rr.getSelfScore() == null ? "" : rr.getSelfScore()
            });
        }
        // 第15行 总分
        AssessmentTableResp hdr = tableService.toTableResp(t, false);

        style.writeDataRow(startRow + 14, new Object[]{

                "总分", "", "", "", hdr.getSelfTotalScore() == null ? "" : hdr.getSelfTotalScore(),
                "最终得分", hdr.getFinalScore() == null ? "" : hdr.getFinalScore(),
                "考核结果", hdr.getGrade() == null ? "" : hdr.getGrade()
        });
        // 第16行 签字栏
        style.writeDataRow(startRow + 15, new Object[]{

                "部门负责人签字确认：", "被考核人签字确认：", "日期：", "", "", "", "", "", ""
        });
        // 数据区外框淡灰（第 4-16 行，第 1-8 列）
        style.setDataBorder(startRow + 3, startRow + 15, 1, 8);
    }

    private void setColumnWidths(ExportStyleUtil style) {

        style.setColumnWidth(0, 18);
        style.setColumnWidth(1, 14);
        style.setColumnWidth(2, 14);
        style.setColumnWidth(3, 30);
        style.setColumnWidth(4, 12);
        style.setColumnWidth(5, 36);
        style.setColumnWidth(6, 36);
        style.setColumnWidth(7, 12);
        style.setColumnWidth(8, 12);
    }

    // 导出打印 HTML
    public String exportPrintHtml(Long periodId) {

        // 查询单条
        AssessmentPeriod period = periodId == null ? null : periodMapper.selectById(periodId);
        // 查询列表
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

            // 查询单条
            SysUser u = userMapper.selectById(t.getUserId());
            // 查询单条
            SysDepartment d = deptMapper.selectById(t.getDeptId());
            // 查询单条
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
            // 查询列表
            List<AssessmentRow> rows = rowMapper.selectList(
                    new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));

            for (AssessmentRow r : rows) {

                // 调用业务服务
                RowResp rr = tableService.toRowResp(r, false);
                String completionRate = rr.getCompletionRate() == null ? "" : rr.getCompletionRate() + "%";
                String selfScore = rr.getSelfScore() == null ? "" : String.valueOf(rr.getSelfScore());
                sb.append("<tr><td>").append(r.getSeq()).append("</td>")
                  .append("<td>").append(r.getCategory()).append("</td>")
                  .append("<td>").append(nullSafe(rr.getIndicatorName())).append("</td>")
                  .append("<td>").append(rr.getBaseScore()).append("</td>")
                  .append("<td>").append(nullSafe(rr.getWorkTarget())).append("</td>")
                  .append("<td>").append(nullSafe(rr.getScoreCriteria())).append("</td>")
                  .append("<td>").append(completionRate).append("</td>")
                  .append("<td>").append(selfScore).append("</td>")
                  .append("</tr>");
            }

            // 调用业务服务
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

    // 空值安全转字符串
    private String nullSafe(Object o) { return o == null ? "" : o.toString().replace("\n", "<br/>"); }
}
