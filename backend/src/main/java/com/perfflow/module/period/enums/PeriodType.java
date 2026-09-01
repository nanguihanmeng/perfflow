package com.perfflow.module.period.enums;
import com.perfflow.common.constant.RoleConst;
import lombok.Getter;
import java.util.Arrays;
import java.util.List;
// 考核周期类型（对应《角色与考核参与矩阵》中的七张表）。
 // 部门线 3 张：年度部门任务分解、季度部门任务调整、年度部门绩效考核评分。
@Getter
public enum PeriodType {

    QUARTER_GOAL("季度目标填报", "个人", false,
            "A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。"),
    QUARTER_ASSESS("岗位季度绩效考核", "个人", false,
            "A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。"),
    ANNUAL_ASSESS("个人年度绩效考核", "个人", true,
            "A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。"),
    BONUS_APPLY("加减分项信息申请", "个人", false,
            "A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(建议全部 BONUS)；D 列指标名称；E 列指标分数(正为加分/负为减分)；F 列工作目标；G 列评分标准。"),
    DEPT_YEAR_TASK("年度部门任务分解", "部门", true,
            "A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。"),
    DEPT_QUARTER_ADJUST("季度部门任务调整", "部门", false,
            "A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。"),
    DEPT_YEAR_SCORE("年度部门绩效考核评分", "部门", true,
            "A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。");
    private final String label;
    private final String line;
    private final boolean annual;
    private final String importFormat;

    PeriodType(String label, String line, boolean annual, String importFormat) {
        this.label = label;
        this.line = line;
        this.annual = annual;
        this.importFormat = importFormat;
    }

    // 是否部门线类型。

    public boolean isDeptLine() {

        return "部门".equals(line);
    }

    // 参与填报的角色集。
     // 部门线：部门绩效专员填报、部门负责人复核（对应部门考核流程）。

    public List<String> participantRoles() {
        // 条件分支处理
        if (isDeptLine()) {

            return Arrays.asList(RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_DEPT_LEAD);
        }
        return Arrays.asList(RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_LEAD, RoleConst.ROLE_LEAD,
                RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION, RoleConst.ROLE_COMMITTEE);
    }

    // 按编码解析，未知编码抛 {@link IllegalArgumentException}。

    public static PeriodType of(String code) {

        for (PeriodType t : values()) {
            if (t.name().equals(code)) {

                return t;
            }
        }

        throw new IllegalArgumentException("非法周期类型: " + code);
    }
}
