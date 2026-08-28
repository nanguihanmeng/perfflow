package com.perfflow.module.period.enums;

import com.perfflow.common.constant.RoleConst;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 考核周期类型（对应《角色与考核参与矩阵》中的七张表）。
 *
 * <p>个人线 4 张：季度目标填报、岗位季度绩效考核、个人年度绩效考核、加减分项信息申请；
 * 部门线 3 张：年度部门任务分解、季度部门任务调整、年度部门绩效考核评分。
 */
@Getter
public enum PeriodType {

    /** 表1 季度目标填报（季初定目标，无分数，纯计划） */
    QUARTER_GOAL("季度目标填报", "个人", false,
            "A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。"),
    /** 表2 岗位季度绩效考核表（季末正式打分） */
    QUARTER_ASSESS("岗位季度绩效考核", "个人", false,
            "A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。"),
    /** 表3 个人年度绩效考核表（年度汇总评定） */
    ANNUAL_ASSESS("个人年度绩效考核", "个人", true,
            "A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(PLAN/OPEN/BONUS)；D 列指标名称；E 列指标分数；F 列工作目标；G 列评分标准。"),
    /** 表4 加减分项信息申请表（公司统一加减分项目库选取，独立修正凭证） */
    BONUS_APPLY("加减分项信息申请", "个人", false,
            "A 列：员工登录名（每员工 10 行首行标注）；B 列序号；C 列指标类别(建议全部 BONUS)；D 列指标名称；E 列指标分数(正为加分/负为减分)；F 列工作目标；G 列评分标准。"),
    /** 表5 年度部门工作任务分解表（部门年度总纲） */
    DEPT_YEAR_TASK("年度部门任务分解", "部门", true,
            "A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。"),
    /** 表6 季度部门工作任务分解（调整）表（表5 的修订版本，记录变更痕迹） */
    DEPT_QUARTER_ADJUST("季度部门任务调整", "部门", false,
            "A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。"),
    /** 表7 年度部门绩效考核评分表（部门年度结果认定） */
    DEPT_YEAR_SCORE("年度部门绩效考核评分", "部门", true,
            "A 列：行类型(经营业绩/运营指标/重点工作)；B 列序号；C 列指标名称；D 列目标值；E 列实际完成值；F 列评分标准；G 列得分；H 列权重(0-100)。");

    /** 中文名称 */
    private final String label;
    /** 所属线：个人 / 部门 */
    private final String line;
    /** 是否为年度周期（年度周期季度固定为 0） */
    private final boolean annual;
    /** 快捷导入格式说明 */
    private final String importFormat;

    PeriodType(String label, String line, boolean annual, String importFormat) {
        this.label = label;
        this.line = line;
        this.annual = annual;
        this.importFormat = importFormat;
    }

    /**
     * 是否部门线类型。
     *
     * @return true 为部门线（表5/6/7）
     */
    public boolean isDeptLine() {
        return "部门".equals(line);
    }

    /**
     * 参与填报的角色集。
     *
     * <p>个人线：EMP/DEPT_LEAD/LEAD/DEPT_STAFF/OPERATION/COMMITTEE（被考核人，ADMIN/HR 不参与）；
     * 部门线：部门绩效专员填报、部门负责人复核（对应部门考核流程）。
     *
     * @return 可勾选的角色编码列表
     */
    public List<String> participantRoles() {
        if (isDeptLine()) {
            return Arrays.asList(RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_DEPT_LEAD);
        }
        return Arrays.asList(RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_LEAD, RoleConst.ROLE_LEAD,
                RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION, RoleConst.ROLE_COMMITTEE);
    }

    /**
     * 按编码解析，未知编码抛 {@link IllegalArgumentException}。
     *
     * @param code 类型编码
     * @return 周期类型
     */
    public static PeriodType of(String code) {
        for (PeriodType t : values()) {
            if (t.name().equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("非法周期类型: " + code);
    }
}
