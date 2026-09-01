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
// 考核计算服务：负责单行自评分、自评总分、最终得分、等级。
 // 纯计算，不读权限。
@Service
@RequiredArgsConstructor
public class AssessmentCalcService {

    private final AssessmentRowMapper rowMapper;
    private final AssessmentTableMapper tableMapper;
    // 计算单行 self_score = base_score * completion_rate / 100（保留 2 位小数）。
     // 调整「自评得分」时作为减分录入，默认不得自动计为负分。

    // 执行业务处理
    public static BigDecimal calcSelfScore(BigDecimal baseScore, BigDecimal completionRate) {

        if (baseScore == null || completionRate == null) return null;
        BigDecimal score = baseScore.multiply(completionRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    // 重算整张主表的自评总分并写入。
     // self_score 由调用方按场景决定：
    @Transactional
    // 执行 recalc。

    // 执行业务处理
    public AssessmentTable recalc(AssessmentTable table) {

        // 判空处理
        if (table == null || table.getId() == null) {

            throw new IllegalArgumentException("table 不能为空");
        }

        // 查询列表
        List<AssessmentRow> rows = rowMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AssessmentRow>()
                        .eq("table_id", table.getId())
                        .orderByAsc("seq"));
        BigDecimal total = BigDecimal.ZERO;

        for (AssessmentRow r : rows) {

            // 非空才处理
            if (r.getSelfScore() != null) {

                total = total.add(r.getSelfScore());
            }
        }

        table.setSelfTotalScore(total.setScale(2, RoundingMode.HALF_UP));
        // 更新记录
        tableMapper.updateById(table);
        return table;
    }

    // 按完成率重算表中各行 self_score，并重算总分。
    @Transactional
    // 执行 recalcByCompletionRate。

    // 执行业务处理
    public AssessmentTable recalcByCompletionRate(AssessmentTable table) {

        // 查询列表
        List<AssessmentRow> rows = rowMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AssessmentRow>()
                        .eq("table_id", table.getId())
                        .orderByAsc("seq"));

        for (AssessmentRow r : rows) {

            // 相等判断
            if (RowCategory.BONUS.name().equals(r.getCategory())) {
                // 加减分项：默认得分 0（指标分数为负数时不得自动计为负分；正数加分项需由部门领导在审核时确认为自评得分）
                r.setSelfScore(BigDecimal.ZERO);

            } else {

                r.setSelfScore(calcSelfScore(r.getBaseScore(), r.getCompletionRate()));
            }

            // 更新记录
            rowMapper.updateById(r);
        }

        return recalc(table);
    }

    // 写入领导评分后计算 final_score 与 grade。
    @Transactional
    // 执行 finalizeWithLeaderScore。

    // 执行业务处理
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
        // 计算等级与总分
        table.setGrade(gradeOf(finalS));
        // 更新记录
        tableMapper.updateById(table);
        return table;
    }

    // 执行业务处理
    public static String gradeOf(BigDecimal finalScore) {

        if (finalScore == null) return null;
        double v = finalScore.doubleValue();
        if (v >= 90) return "A";
        if (v >= 75) return "B";
        if (v >= 60) return "C";
        return "D";
    }
}
