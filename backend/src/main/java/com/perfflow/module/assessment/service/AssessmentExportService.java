package com.perfflow.module.assessment.service;
import cn.hutool.http.HtmlUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.common.excel.ExportStyleUtil;
import com.perfflow.module.assessment.dto.AssessmentTableResp;
import com.perfflow.module.assessment.dto.RowResp;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
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
    // 导出某周期已完成考核主表为 Excel（模板 A1:G16 格式），
     // 未完成填报的人员在表格最下方以小字说明。

     // 导出考核 Excel
    public byte[] exportExcel(Long periodId) {

        // 校验必须指定周期
        if (periodId == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "请选择要导出的考核周期");
        }

        // 查询单条
        AssessmentPeriod period = periodMapper.selectById(periodId);

        // 判空处理
        if (period == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.NOT_FOUND, "考核周期不存在");
        }

        // 查询列表
        List<AssessmentTable> allTables = tableMapper.selectList(
                new QueryWrapper<AssessmentTable>()
                        .eq("period_id", periodId)
                        .orderByAsc("dept_id").orderByAsc("user_id"));
        // 仅导出已完成（FINISHED）填报的主表
        List<AssessmentTable> tables = allTables.stream()
                .filter(t -> AssessmentState.FINISHED.name().equals(t.getState()))
                .toList();
        // 未完成填报人员（含状态），用于底部说明
        List<AssessmentTable> unfinished = allTables.stream()
                .filter(t -> !AssessmentState.FINISHED.name().equals(t.getState()))
                .toList();
        // 批量加载用户/部门/部门负责人姓名，避免逐表查询
        ExportContext ctx = loadContext(allTables);

        try (ExcelWriter writer = ExcelUtil.getWriter(true);

             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ExportStyleUtil style = new ExportStyleUtil(writer);
            setColumnWidths(style);
            int idx = 0;

            for (AssessmentTable t : tables) {

                writeTable(writer, style, t, period, idx, ctx.userNames, ctx.deptNames, ctx.deptLeadNames);
                idx += 20; // 每表占 16 行 + 4 空行
            }

            // 未完成填报人员说明（表格最下方，小字灰色）
            if (!unfinished.isEmpty()) {

                writeUnfinishedNote(style, idx, unfinished, ctx.userNames, ctx.deptNames);
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

    // 在表格最下方写入未完成填报人员说明（小字灰色）。
    private void writeUnfinishedNote(ExportStyleUtil style, int startRow,
                                     List<AssessmentTable> unfinished,
                                     // 构建集合容器
                                     Map<Long, String> userCache, Map<Long, String> deptCache) {

        // 标题行
        style.writeNote(startRow, 1, "以下人员尚未完成本周期填报，未纳入上表：");
        startRow++;

        for (AssessmentTable t : unfinished) {

            String name = userCache.getOrDefault(t.getUserId(), "");
            String dept = deptCache.getOrDefault(t.getDeptId(), "");
            String stateLabel = stateLabel(t.getState());
            // 逐行列出
            style.writeNote(startRow++, 1, "· " + dept + " - " + name + "（" + stateLabel + "）");
        }
    }

    // 未完成状态转中文标签
    private String stateLabel(String state){

        if (state == null){ return "未开始";}

        // 条件分支
        switch (state) {

            case "SELF_SUSPENDED":

                return "挂起中";

            case "DEPT_REVIEW":

                return "待部门审核";

            case "LEAD_SCORING":

                return "待领导评分";

            case "SELF_DRAFTING":

                return "填报中";

            default:

                return state;
        }
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

        // 校验必须指定周期
        if (periodId == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "请选择要导出的考核周期");
        }

        // 查询单条
        AssessmentPeriod period = periodMapper.selectById(periodId);

        // 判空处理
        if (period == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.NOT_FOUND, "考核周期不存在");
        }

        // 查询列表
        List<AssessmentTable> allTables = tableMapper.selectList(
                new QueryWrapper<AssessmentTable>()
                        .eq("period_id", periodId)
                        .orderByAsc("dept_id").orderByAsc("user_id"));
        // 仅导出已完成（FINISHED）填报的主表
        List<AssessmentTable> tables = allTables.stream()
                .filter(t -> AssessmentState.FINISHED.name().equals(t.getState()))
                .toList();
        // 未完成填报人员（含状态），用于底部说明
        List<AssessmentTable> unfinished = allTables.stream()
                .filter(t -> !AssessmentState.FINISHED.name().equals(t.getState()))
                .toList();
        // 批量加载用户/部门/部门负责人姓名，避免逐表查询
        ExportContext ctx = loadContext(allTables);
        String periodName = period.getName() == null ? "" : period.getName();

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='utf-8'><title>")
          .append(escHtml(periodName)).append("</title>")
          .append("<style>")
          .append("body{font-family:Arial,sans-serif;}table{border-collapse:collapse;width:100%;margin-bottom:20px;}")
          .append("th,td{border:1px solid #000;padding:6px 10px;}th{background:#f4f4f4;}h2{margin:8px 0;}")
          .append(".title{font-size:18px;font-weight:bold;text-align:center;margin:8px 0;}")
          .append(".info{font-size:13px;margin:4px 0;}")
          .append(".note{font-size:12px;color:#808080;margin:4px 0;}")
          .append("@media print { .no-print{display:none;} }")
          .append("</style></head><body>");
        sb.append("<div class='no-print'><button onclick='window.print()'>打印</button></div>");
        sb.append("<h1>").append(escHtml(periodName)).append("</h1>");

        for (AssessmentTable t : tables) {

            String realName = ctx.userNames.getOrDefault(t.getUserId(), "");
            String deptName = ctx.deptNames.getOrDefault(t.getDeptId(), "");
            String deptLeadName = ctx.deptLeadNames.getOrDefault(t.getDeptId(), "");
            String position = t.getPosition() == null ? "" : t.getPosition();
            sb.append("<div class='title'>岗位季度绩效考核表</div>");
            sb.append("<div class='info'>部门：").append(escHtml(deptName))
              .append("　被考核人：").append(escHtml(realName))
              .append("　岗位：").append(escHtml(position))
              .append("　部门负责人：").append(escHtml(deptLeadName)).append("</div>");
            sb.append("<div class='info'>考核期：").append(escHtml(periodName))
              .append("　填表日期：</div>");
            sb.append("<table><thead><tr><th>序号</th><th>指标类别</th><th>指标名称</th><th>指标分数</th>")
              .append("<th>工作目标</th><th>评分标准</th><th>完成率</th><th>自评得分</th></tr></thead><tbody>");
            // 查询列表
            List<AssessmentRow> rows = rowMapper.selectList(
                    new QueryWrapper<AssessmentRow>().eq("table_id", t.getId()).orderByAsc("seq"));

            for (AssessmentRow r : rows) {

                // 调用业务服务
                RowResp rr = tableService.toRowResp(r, false);
                sb.append("<tr><td>").append(escHtml(r.getSeq())).append("</td>")
                  .append("<td>").append(escHtml(r.getCategory())).append("</td>")
                  .append("<td>").append(escHtml(rr.getIndicatorName())).append("</td>")
                  .append("<td>").append(escHtml(rr.getBaseScore())).append("</td>")
                  .append("<td>").append(escHtml(rr.getWorkTarget())).append("</td>")
                  .append("<td>").append(escHtml(rr.getScoreCriteria())).append("</td>")
                  .append("<td>").append(escHtml(rr.getCompletionRate() == null ? null
                          : rr.getCompletionRate() + "%")).append("</td>")
                  .append("<td>").append(escHtml(rr.getSelfScore())).append("</td>")
                  .append("</tr>");
            }

            // 调用业务服务
            AssessmentTableResp hdr = tableService.toTableResp(t, false);
            sb.append("<tr><td colspan='4'>总分</td><td>")
              .append(escHtml(hdr.getSelfTotalScore())).append("</td>")
              .append("<td>最终得分</td><td>").append(escHtml(hdr.getFinalScore()))
              .append("</td><td>考核结果</td><td>").append(escHtml(hdr.getGrade()))
              .append("</td></tr>");
            sb.append("</tbody></table>");
            sb.append("<div class='info'>部门负责人签字确认：　　　　被考核人签字确认：　　　　日期：</div>");
        }

        // 未完成填报人员说明（页面最下方，小字灰色）
        if (!unfinished.isEmpty()) {

            sb.append("<div class='note'>以下人员尚未完成本周期填报，未纳入上表：</div>");

            for (AssessmentTable t : unfinished) {

                String name = ctx.userNames.getOrDefault(t.getUserId(), "");
                String dept = ctx.deptNames.getOrDefault(t.getDeptId(), "");
                // 逐行列出
                sb.append("<div class='note'>· ").append(escHtml(dept)).append(" - ").append(escHtml(name))
                  .append("（").append(escHtml(stateLabel(t.getState()))).append("）</div>");
            }
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    // 空值安全转字符串（Excel 单元格使用，不做 HTML 转义）
    private String nullSafe(Object o) { return o == null ? "" : o.toString().replace("\n", "<br/>"); }

    // 打印 HTML 转义 + 换行转 <br/>（用户可控文本转义，防 XSS）
    private static String escHtml(Object v) {

        if (v == null) {

            return "";
        }
        return HtmlUtil.escape(String.valueOf(v)).replace("\n", "<br/>");
    }

    // 导出缓存：用户真实姓名 / 部门名 / 部门负责人姓名，供 Excel 与打印 HTML 共用。
    private static class ExportContext {

        private final Map<Long, String> userNames = new HashMap<>();
        private final Map<Long, String> deptNames = new HashMap<>();
        private final Map<Long, String> deptLeadNames = new HashMap<>();
    }

    // 批量加载用户真实姓名、部门名与部门负责人姓名，避免逐表 N+1 查询。
    private ExportContext loadContext(List<AssessmentTable> tables) {

        ExportContext ctx = new ExportContext();

        for (AssessmentTable t : tables) {

            // 非空才处理
            if (t.getUserId() != null && !ctx.userNames.containsKey(t.getUserId())) {

                // 查询单条
                SysUser u = userMapper.selectById(t.getUserId());
                ctx.userNames.put(t.getUserId(), u == null ? "" : u.getRealName());
            }

            // 非空才处理
            if (t.getDeptId() != null && !ctx.deptNames.containsKey(t.getDeptId())) {

                // 查询单条
                SysDepartment d = deptMapper.selectById(t.getDeptId());
                ctx.deptNames.put(t.getDeptId(), d == null ? "" : d.getName());
                // 查询单条
                SysUser lead = userMapper.selectOne(new QueryWrapper<SysUser>()
                        .eq("dept_id", t.getDeptId()).eq("role", RoleConst.ROLE_DEPT_LEAD)
                        .eq("status", 1).last("LIMIT 1"));
                ctx.deptLeadNames.put(t.getDeptId(), lead == null ? "" : lead.getRealName());
            }
        }
        // 返回结果
        return ctx;
    }
}
