package com.perfflow.module.assessment.enums;

import lombok.Getter;

/**
 * 考核主表状态机枚举。
 */
@Getter
public enum AssessmentState {

    /** 1 自评中 */
    SELF_DRAFTING,
    /** 2 自评挂起（等待人事确认推送） */
    SELF_SUSPENDED,
    /** 3 部门审核 */
    DEPT_REVIEW,
    /** 4 领导评分 */
    LEAD_SCORING,
    /** 5 已完成 */
    FINISHED
}
