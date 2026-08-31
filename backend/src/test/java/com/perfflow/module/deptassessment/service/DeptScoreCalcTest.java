package com.perfflow.module.deptassessment.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 部门 KPI 行得分自动计算公式覆盖。
 */
class DeptScoreCalcTest {

    @Test
    void full_completion_scores_full_weight() {
        BigDecimal score = DeptAssessmentService.calcRowScore("100", "100", new BigDecimal("40"));
        assertEquals(new BigDecimal("40.00"), score);
    }

    @Test
    void partial_completion_scores_by_ratio() {
        BigDecimal score = DeptAssessmentService.calcRowScore("100", "50", new BigDecimal("40"));
        assertEquals(new BigDecimal("20.00"), score);
    }

    @Test
    void over_completion_is_capped_at_one_hundred_percent() {
        BigDecimal score = DeptAssessmentService.calcRowScore("100", "150", new BigDecimal("40"));
        assertEquals(new BigDecimal("40.00"), score);
    }

    @Test
    void zero_actual_scores_zero() {
        BigDecimal score = DeptAssessmentService.calcRowScore("100", "0", new BigDecimal("40"));
        assertEquals(new BigDecimal("0.00"), score);
    }

    @Test
    void rounding_to_two_decimals() {
        BigDecimal score = DeptAssessmentService.calcRowScore("3", "1", new BigDecimal("60"));
        // 1/3*60 = 20.0
        assertEquals(new BigDecimal("20.00"), score);
        BigDecimal score2 = DeptAssessmentService.calcRowScore("7", "1", new BigDecimal("70"));
        // 1/7*70 = 10.0
        assertEquals(new BigDecimal("10.00"), score2);
    }

    @Test
    void non_numeric_target_scores_zero() {
        BigDecimal score = DeptAssessmentService.calcRowScore("完成率≥95%", "达标", new BigDecimal("40"));
        assertEquals(BigDecimal.ZERO, score);
    }

    @Test
    void missing_actual_scores_zero() {
        BigDecimal score = DeptAssessmentService.calcRowScore("100", null, new BigDecimal("40"));
        assertEquals(BigDecimal.ZERO, score);
    }

    @Test
    void null_weight_scores_zero() {
        BigDecimal score = DeptAssessmentService.calcRowScore("100", "100", null);
        assertEquals(BigDecimal.ZERO, score);
    }

    @Test
    void zero_target_scores_zero() {
        BigDecimal score = DeptAssessmentService.calcRowScore("0", "100", new BigDecimal("40"));
        assertEquals(BigDecimal.ZERO, score);
    }

    @Test
    void negative_target_scores_zero() {
        BigDecimal score = DeptAssessmentService.calcRowScore("-10", "100", new BigDecimal("40"));
        assertEquals(BigDecimal.ZERO, score);
    }
}
