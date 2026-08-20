package com.perfflow.module.assessment.enums;

import lombok.Getter;

/**
 * 行的分类。
 */
@Getter
public enum RowCategory {

    /** 个人季度工作计划 (seq 1-5) */
    PLAN,
    /** 开放型指标 (seq 6-7) */
    OPEN,
    /** 加减分项 (seq 8-10) */
    BONUS
}
