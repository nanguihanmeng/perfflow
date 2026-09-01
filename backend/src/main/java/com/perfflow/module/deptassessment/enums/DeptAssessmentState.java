package com.perfflow.module.deptassessment.enums;
import lombok.Getter;
// 部门考核状态机状态（数字编码，与 assessment_period.status 风格一致）。
@Getter
public enum DeptAssessmentState {

    NOT_STARTED(0),
    SELF_FILLING(1),
    PENDING_REVIEW(2),
    PENDING_AUDIT(3),
    PENDING_APPROVE(4),
    COMPLETED(5);
    // 编码
    private final int code;

    DeptAssessmentState(int code) {
        this.code = code;
    }

    public static DeptAssessmentState of(int code) {

        for (DeptAssessmentState s : values()) {
            if (s.code == code) {

                return s;
            }
        }

        throw new IllegalArgumentException("非法部门考核状态: " + code);
    }
}
