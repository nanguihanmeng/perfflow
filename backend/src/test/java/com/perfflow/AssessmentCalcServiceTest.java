package com.perfflow.module.assessment;

import com.perfflow.module.assessment.service.AssessmentCalcService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 纯计算公式测试。
 */
class AssessmentCalcServiceTest {

    @Test
    void selfScore_basic() {
        // base 16 * 100% = 16.00
        BigDecimal s = AssessmentCalcService.calcSelfScore(
                new BigDecimal("16"), new BigDecimal("100"));
        assertEquals(0, s.compareTo(new BigDecimal("16.00")));
    }

    @Test
    void selfScore_120() {
        // base 16 * 120% = 19.20
        BigDecimal s = AssessmentCalcService.calcSelfScore(
                new BigDecimal("16"), new BigDecimal("120"));
        assertEquals(0, s.compareTo(new BigDecimal("19.20")));
    }

    @Test
    void selfScore_round() {
        // base 10 * 33% = 3.30
        BigDecimal s = AssessmentCalcService.calcSelfScore(
                new BigDecimal("10"), new BigDecimal("33"));
        assertEquals(0, s.compareTo(new BigDecimal("3.30")));
    }

    @Test
    void grade_A() {
        assertEquals("A", AssessmentCalcService.gradeOf(new BigDecimal("90")));
        assertEquals("A", AssessmentCalcService.gradeOf(new BigDecimal("100")));
    }

    @Test
    void grade_B() {
        // 新阈值：B 为 75-90
        assertEquals("B", AssessmentCalcService.gradeOf(new BigDecimal("89.99")));
        assertEquals("B", AssessmentCalcService.gradeOf(new BigDecimal("75")));
    }

    @Test
    void grade_C() {
        // 新阈值：C 为 60-75
        assertEquals("C", AssessmentCalcService.gradeOf(new BigDecimal("74.99")));
        assertEquals("C", AssessmentCalcService.gradeOf(new BigDecimal("60")));
    }

    @Test
    void grade_D() {
        // 新阈值：D < 60
        assertEquals("D", AssessmentCalcService.gradeOf(new BigDecimal("59.99")));
        assertEquals("D", AssessmentCalcService.gradeOf(new BigDecimal("0")));
    }
}
