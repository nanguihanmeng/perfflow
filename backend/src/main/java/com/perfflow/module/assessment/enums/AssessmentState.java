package com.perfflow.module.assessment.enums;
import lombok.Getter;
// 考核主表状态机枚举。
@Getter
public enum AssessmentState {

    SELF_DRAFTING,
    SELF_SUSPENDED,
    DEPT_REVIEW,
    LEAD_SCORING,
    FINISHED
}
