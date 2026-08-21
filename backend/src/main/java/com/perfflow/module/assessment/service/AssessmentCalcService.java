package com.perfflow.module.assessment.service;

import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.RowCategory;
import com.perfflow.module.assessment.mapper.AssessmentRowMapper;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 考核计算服务：负责单行自评分、自评总分、最终得分、等级。
 * 纯计算，不读权限。
 */
@Service
@RequiredArgsConstructor
public class AssessmentCalcService {

    private final AssessmentRowMapper rowMapper;
    private final AssessmentTableMapper tableMapper;

    /**
     * 计算单行 self_score = base_score * completion_rate / 100（保留 2 位小数）。
     */
    public static BigDecimal calcSelfScore(BigDecimal baseScore, BigDecimal completionRate) {
        if (baseScore == null || completionRate == null) return null;
        BigDecimal r = baseScore.multiply(completionRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return r.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 重算整张主表的自评总分并写入。
     *
     * <p>规则：只对已存在的 self_score 求和（BONUS 行不计入），
     * 不重算 self_score —— self_score 由调用方按场景决定：
     * <ul>
     *   <li>员工改完成率：先按「指标分数 × 完成率%」重算各非 BONUS 行 self_score，再求和</li>
     *   <li>部门领导改自评得分：直接覆盖 self_score，再求和</li>
     * </ul>
     */
    @Transactional
    public AssessmentTable recalc(AssessmentTable table) {
        List<AssessmentRow> rows = rowMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AssessmentRow>()
                        .eq("table_id", table.getId())
                        .orderByAsc("seq"));

        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentRow r : rows) {
            if (RowCategory.BONUS.name().equals(r.getCategory())) {
                // BONUS 行不计入自评总分
                continue;
            }
            if (r.getSelfScore() != null) {
                total = total.add(r.getSelfScore());
            }
        }
        table.setSelfTotalScore(total.setScale(2, RoundingMode.HALF_UP));
        tableMapper.updateById(table);
        return table;
    }

    /**
     * 按完成率重算表中所有非 BONUS 行的 self_score，并重算总分。
     *
     * <p>用于员工填写完成率/提交时。
     */
    @Transactional
    public AssessmentTable recalcByCompletionRate(AssessmentTable table) {
        List<AssessmentRow> rows = rowMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AssessmentRow>()
                        .eq("table_id", table.getId())
                        .orderByAsc("seq"));
        for (AssessmentRow r : rows) {
            if (RowCategory.BONUS.name().equals(r.getCategory())) {
                r.setSelfScore(null);
            } else {
                r.setSelfScore(calcSelfScore(r.getBaseScore(), r.getCompletionRate()));
            }
            rowMapper.updateById(r);
        }
        return recalc(table);
    }

    /**
     * 写入领导评分后计算 final_score 与 grade。
     */
    @Transactional
    public AssessmentTable finalizeWithLeaderScore(AssessmentTable table, BigDecimal leaderScore) {
        if (leaderScore == null || leaderScore.compareTo(BigDecimal.ZERO) < 0
                || leaderScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("leaderScore out of range");
        }
        table.setLeaderScore(leaderScore.setScale(2, RoundingMode.HALF_UP));
        BigDecimal self = table.getSelfTotalScore() == null ? BigDecimal.ZERO : table.getSelfTotalScore();
        BigDecimal finalS = self.multiply(BigDecimal.valueOf(0.5))
                .add(leaderScore.multiply(BigDecimal.valueOf(0.5)))
                .setScale(2, RoundingMode.HALF_UP);
        table.setFinalScore(finalS);
        table.setGrade(gradeOf(finalS));
        tableMapper.updateById(table);
        return table;
    }

    /** A>=90, B>=75, C>=60, D<60 */
    public static String gradeOf(BigDecimal finalScore) {
        if (finalScore == null) return null;
        double v = finalScore.doubleValue();
        if (v >= 90) return "A";
        if (v >= 75) return "B";
        if (v >= 60) return "C";
        return "D";
    }
}
