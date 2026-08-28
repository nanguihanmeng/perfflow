package com.perfflow.module.deptassessment.enums;

import lombok.Getter;

/**
 * 部门考核状态机状态（数字编码，与 assessment_period.status 风格一致）。
 *
 * <p>流转：NOT_STARTED(0) → SELF_FILLING(1) → PENDING_REVIEW(2) → PENDING_AUDIT(3) → PENDING_APPROVE(4) → COMPLETED(5)
 */
@Getter
public enum DeptAssessmentState {

    /** 未开始 */
    NOT_STARTED(0),
    /** 自评中（部门绩效专员填报 KPI） */
    SELF_FILLING(1),
    /** 待复核（部门负责人复核） */
    PENDING_REVIEW(2),
    /** 待初审（运营管理部初审） */
    PENDING_AUDIT(3),
    /** 待审批（绩效委员会最终审批） */
    PENDING_APPROVE(4),
    /** 已完成（触发等级自动计算） */
    COMPLETED(5);

    private final int code;

    DeptAssessmentState(int code) {
        this.code = code;
    }

    /** 按编码取枚举，非法编码抛 IllegalArgumentException */
    public static DeptAssessmentState of(int code) {
        for (DeptAssessmentState s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("非法部门考核状态: " + code);
    }
}
