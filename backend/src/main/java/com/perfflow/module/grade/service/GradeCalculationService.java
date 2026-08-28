package com.perfflow.module.grade.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.mapper.DeptAssessmentMapper;
import com.perfflow.module.grade.entity.GradeQuotaConfig;
import com.perfflow.module.grade.entity.WeightConfig;
import com.perfflow.module.grade.mapper.GradeQuotaConfigMapper;
import com.perfflow.module.grade.mapper.WeightConfigMapper;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * 等级联动计算服务。
 *
 * <p>算法（按部门等级 + 员工层级 → 名额比例 → 按 final_score 排名填充）：
 * <ol>
 *   <li>获取部门等级（dept_assessment.dept_grade）</li>
 *   <li>按部门等级 + 员工层级查 grade_quota_config 得到 A/B/C/D 比例</li>
 *   <li>计算本部门该层级实际人数 × 比例 → 各等级名额</li>
 *   <li>按 final_score 从高到低排名，依次填充 A → B → C → D</li>
 *   <li>写入 assessment_table 的 grade 与 quota_grade_a/b/c/d</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeCalculationService {

    /** 员工层级 */
    public static final String LEVEL_MIDDLE = "MIDDLE";
    public static final String LEVEL_BASIC = "BASIC";

    private final AssessmentTableMapper tableMapper;
    private final DeptAssessmentMapper deptAssessmentMapper;
    private final GradeQuotaConfigMapper quotaMapper;
    private final WeightConfigMapper weightMapper;
    private final SysUserMapper userMapper;

    /**
     * 对某周期执行等级自动计算（运营管理部触发）。
     *
     * @param periodId 周期ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoCalculate(Long periodId) {
        // 1. 该周期所有已完成的个人考核
        List<AssessmentTable> tables = tableMapper.selectList(new QueryWrapper<AssessmentTable>()
                .eq("period_id", periodId)
                .eq("state", AssessmentState.FINISHED.name()));

        // 2. 该周期部门考核
        List<DeptAssessment> deptAssessments = deptAssessmentMapper.selectList(
                new QueryWrapper<DeptAssessment>().eq("period_id", periodId));

        for (AssessmentTable t : tables) {
            DeptAssessment dept = findDeptAssessment(deptAssessments, t.getDeptId());
            if (dept == null || dept.getDeptGrade() == null) {
                // 部门等级未出，跳过该员工
                continue;
            }
            String staffLevel = resolveStaffLevel(t);
            GradeQuotaConfig quota = quotaMapper.selectOne(new QueryWrapper<GradeQuotaConfig>()
                    .eq("dept_grade", dept.getDeptGrade())
                    .eq("staff_level", staffLevel)
                    .last("LIMIT 1"));
            if (quota == null) {
                continue;
            }
            // 3. 写部门等级与权重快照
            WeightConfig weight = weightMapper.selectOne(new QueryWrapper<WeightConfig>()
                    .eq("staff_level", staffLevel)
                    .orderByDesc("id").last("LIMIT 1"));
            t.setDeptGrade(dept.getDeptGrade());
            if (weight != null) {
                t.setDeptScoreWeight(weight.getDeptWeight());
                t.setPersonalScoreWeight(weight.getPersonalWeight());
                // 4. 中层权重匹配：final = (dept.total×deptWeight + self×personalWeight)/100
                if (LEVEL_MIDDLE.equals(staffLevel) && t.getFinalScore() != null) {
                    BigDecimal deptPart = dept.getTotalScore().multiply(weight.getDeptWeight());
                    BigDecimal personalPart = t.getFinalScore().multiply(weight.getPersonalWeight());
                    BigDecimal weighted = deptPart.add(personalPart)
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    t.setFinalScore(weighted);
                }
            }
            tableMapper.updateById(t);
        }

        // 5. 按部门分组计算名额并填充等级
        assignGradesByDept(tables, periodId);
        log.info("等级自动计算完成: periodId={}, 处理考核表数={}", periodId, tables.size());
    }

    /** 按部门分组，计算各等级名额并按分数排名填充 */
    private void assignGradesByDept(List<AssessmentTable> tables, Long periodId) {
        // 按 dept_id 分组
        java.util.Map<Long, List<AssessmentTable>> byDept = new java.util.HashMap<>();
        for (AssessmentTable t : tables) {
            if (t.getDeptGrade() == null) {
                continue;
            }
            byDept.computeIfAbsent(t.getDeptId(), k -> new java.util.ArrayList<>()).add(t);
        }

        for (java.util.Map.Entry<Long, List<AssessmentTable>> entry : byDept.entrySet()) {
            Long deptId = entry.getKey();
            List<AssessmentTable> deptTables = entry.getValue();
            // 按层级分组
            java.util.Map<String, List<AssessmentTable>> byLevel = new java.util.HashMap<>();
            for (AssessmentTable t : deptTables) {
                byLevel.computeIfAbsent(resolveStaffLevel(t), k -> new java.util.ArrayList<>()).add(t);
            }

            for (java.util.Map.Entry<String, List<AssessmentTable>> levelEntry : byLevel.entrySet()) {
                String staffLevel = levelEntry.getKey();
                List<AssessmentTable> levelTables = levelEntry.getValue();
                // 按 final_score 降序排名
                levelTables.sort(Comparator.comparing(
                        (AssessmentTable t) -> t.getFinalScore() == null ? BigDecimal.ZERO : t.getFinalScore(),
                        Comparator.reverseOrder()));

                String deptGrade = levelTables.get(0).getDeptGrade();
                GradeQuotaConfig quota = quotaMapper.selectOne(new QueryWrapper<GradeQuotaConfig>()
                        .eq("dept_grade", deptGrade)
                        .eq("staff_level", staffLevel)
                        .last("LIMIT 1"));
                if (quota == null) {
                    continue;
                }
                int size = levelTables.size();
                int quotaA = calcQuota(size, quota.getGradeARatio());
                int quotaB = calcQuota(size, quota.getGradeBRatio());
                int quotaC = calcQuota(size, quota.getGradeCRatio());
                int quotaD = size - quotaA - quotaB - quotaC;

                // 填充等级（边界：名额不足时依次降级）
                fillGrades(levelTables, quotaA, quotaB, quotaC, quotaD, deptGrade, periodId);
            }
        }
    }

    /** 按比例计算名额（向下取整，至少保证总数正确） */
    private int calcQuota(int total, BigDecimal ratio) {
        if (ratio == null) {
            return 0;
        }
        return BigDecimal.valueOf(total).multiply(ratio)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).intValue();
    }

    /** 按排名填充 A/B/C/D */
    private void fillGrades(List<AssessmentTable> tables, int quotaA, int quotaB, int quotaC, int quotaD,
                            String deptGrade, Long periodId) {
        int idx = 0;
        for (AssessmentTable t : tables) {
            String grade;
            if (idx < quotaA) {
                grade = "A";
            } else if (idx < quotaA + quotaB) {
                grade = "B";
            } else if (idx < quotaA + quotaB + quotaC) {
                grade = "C";
            } else {
                grade = "D";
            }
            tableMapper.update(null, new LambdaUpdateWrapper<AssessmentTable>()
                    .eq(AssessmentTable::getId, t.getId())
                    .set(AssessmentTable::getGrade, grade)
                    .set(AssessmentTable::getQuotaGradeA, quotaA)
                    .set(AssessmentTable::getQuotaGradeB, quotaB)
                    .set(AssessmentTable::getQuotaGradeC, quotaC)
                    .set(AssessmentTable::getQuotaGradeD, quotaD));
            idx++;
        }
        log.info("等级填充完成: deptId 该层级人数={}, A={}, B={}, C={}, D={}, 部门等级={}",
                tables.size(), quotaA, quotaB, quotaC, quotaD, deptGrade);
    }

    private DeptAssessment findDeptAssessment(List<DeptAssessment> list, Long deptId) {
        for (DeptAssessment d : list) {
            if (d.getDeptId().equals(deptId)) {
                return d;
            }
        }
        return null;
    }

    /**
     * 解析员工层级（按被考核人角色）：
     * 部门领导(DEPT_LEAD) → MIDDLE（最终分=部门分×权重+个人分×权重）；
     * 员工等其余角色 → BASIC。
     *
     * @param t 个人考核主表
     * @return 层级编码 MIDDLE/BASIC
     */
    private String resolveStaffLevel(AssessmentTable t) {
        SysUser u = t.getUserId() == null ? null : userMapper.selectById(t.getUserId());
        if (u != null && RoleConst.ROLE_DEPT_LEAD.equals(u.getRole())) {
            return LEVEL_MIDDLE;
        }
        return LEVEL_BASIC;
    }
}
