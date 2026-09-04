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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.perfflow.module.notification.service.NotificationService;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
// 等级联动计算服务。
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeCalculationService {

    public static final String LEVEL_MIDDLE = "MIDDLE";
    public static final String LEVEL_BASIC = "BASIC";
    private final AssessmentTableMapper tableMapper;
    private final DeptAssessmentMapper deptAssessmentMapper;
    private final GradeQuotaConfigMapper quotaMapper;
    private final WeightConfigMapper weightMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;
    // 对某周期执行等级自动计算（运营管理部触发）。
    @Transactional(rollbackFor = Exception.class)
    // 对指定周期执行等级自动计算。
    // 异步执行等级自动计算，完成后站内信通知绩效考核管理员
    @Async("asyncExecutor")
    public void autoCalculateAsync(Long periodId) {
        try {

            autoCalculate(periodId);
            // 发送站内通知给绩效考核管理员
            notificationService.sendToRole(RoleConst.ROLE_PERFORMANCE_HR, "等级自动计算完成",
                    "周期 " + periodId + " 的等级已自动计算完成，请查看结果", "SYSTEM");

        } catch (Exception e) {

            log.error("异步等级计算失败: periodId={}", periodId, e);
            // 发送站内通知给绩效考核管理员
            notificationService.sendToRole(RoleConst.ROLE_PERFORMANCE_HR, "等级自动计算失败",
                    "周期 " + periodId + " 的等级自动计算失败，请检查数据", "SYSTEM");
        }
    }

    // 触发等级自动计算
    public void autoCalculate(Long periodId) {
        // 1. 该周期所有已完成的个人考核
        List<AssessmentTable> tables = tableMapper.selectList(new QueryWrapper<AssessmentTable>()
                .eq("period_id", periodId)
                .eq("state", AssessmentState.FINISHED.name()));
        // 2. 该周期部门考核
        List<DeptAssessment> deptAssessments = deptAssessmentMapper.selectList(
                new QueryWrapper<DeptAssessment>().eq("period_id", periodId));

        // 批量加载被考核人角色，供员工层级判定，避免逐表查询
        Map<Long, String> roleByUserId = new HashMap<>();
        List<Long> userIds = tables.stream().map(AssessmentTable::getUserId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(userIds)) {
                roleByUserId.put(u.getId(), u.getRole());
            }
        }

        for (AssessmentTable t : tables) {

            DeptAssessment dept = findDeptAssessment(deptAssessments, t.getDeptId());

            // 判空处理
            if (dept == null || dept.getDeptGrade() == null) {
                // 部门等级未出，跳过该员工
                continue;
            }

            // 解析员工层级
            String staffLevel = resolveStaffLevel(t, roleByUserId);
            // 查询单条
            GradeQuotaConfig quota = quotaMapper.selectOne(new QueryWrapper<GradeQuotaConfig>()
                    .eq("dept_grade", dept.getDeptGrade())
                    .eq("staff_level", staffLevel)
                    .last("LIMIT 1"));

            // 判空处理
            if (quota == null) {

                continue;
            }
            // 3. 写部门等级冗余（BASIC 与 MIDDLE 均写，供按部门排名定位）
            t.setDeptGrade(dept.getDeptGrade());

            // 4. 仅中层（部门领导）混算部门分：final = (部门分×部门权重 + 个人分×个人权重) / 100。
            // 个人分恒由 self/leader 子项重算（与考核收口 finalizeWithLeaderScore 同口径），不读回 final_score——
            // final_score 可能已是历史混算结果，读回会把已叠加的部门分再当个人分二次叠加；
            // 部门总分更新后重复触发，可基于最新部门分正确刷新（FINISHED 终态不可改，个人分子项不会变）。
            if (LEVEL_MIDDLE.equals(staffLevel)) {

                // 查询单条
                WeightConfig weight = weightMapper.selectOne(new QueryWrapper<WeightConfig>()
                        .eq("staff_level", staffLevel)
                        .orderByDesc("id").last("LIMIT 1"));

                // 非空才处理
                if (weight != null) {

                    // 权重快照与最终分同源同写，避免出现快照与结果不一致
                    t.setDeptScoreWeight(weight.getDeptWeight());
                    t.setPersonalScoreWeight(weight.getPersonalWeight());
                    BigDecimal personal = personalComposite(t);

                    // 非空才处理
                    if (personal != null && dept.getTotalScore() != null) {

                        BigDecimal deptPart = dept.getTotalScore().multiply(weight.getDeptWeight());
                        BigDecimal personalPart = personal.multiply(weight.getPersonalWeight());
                        BigDecimal weighted = deptPart.add(personalPart)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        t.setFinalScore(weighted);
                    }
                }
            }

            // 更新记录
            tableMapper.updateById(t);
        }

        // 5. 按部门分组计算名额并填充等级
        assignGradesByDept(tables, periodId, roleByUserId);
        log.info("等级自动计算完成: periodId={}, 处理考核表数={}", periodId, tables.size());
    }

    private void assignGradesByDept(List<AssessmentTable> tables, Long periodId,
                                    Map<Long, String> roleByUserId) {
        // 按 dept_id 分组
        java.util.Map<Long, List<AssessmentTable>> byDept = new java.util.HashMap<>();

        for (AssessmentTable t : tables) {

            // 判空处理
            if (t.getDeptGrade() == null) {

                continue;
            }

            byDept.computeIfAbsent(t.getDeptId(), k -> new java.util.ArrayList<>()).add(t);
        }

        for (java.util.Map.Entry<Long, List<AssessmentTable>> entry : byDept.entrySet()) {

            Long deptId = entry.getKey();
            // 构建集合容器
            List<AssessmentTable> deptTables = entry.getValue();
            // 按层级分组
            java.util.Map<String, List<AssessmentTable>> byLevel = new java.util.HashMap<>();

            for (AssessmentTable t : deptTables) {

                // 解析员工层级
                byLevel.computeIfAbsent(resolveStaffLevel(t, roleByUserId), k -> new java.util.ArrayList<>()).add(t);
            }

            for (java.util.Map.Entry<String, List<AssessmentTable>> levelEntry : byLevel.entrySet()) {

                String staffLevel = levelEntry.getKey();
                // 构建集合容器
                List<AssessmentTable> levelTables = levelEntry.getValue();
                // 按 final_score 降序排名
                levelTables.sort(Comparator.comparing(
                        (AssessmentTable t) -> t.getFinalScore() == null ? BigDecimal.ZERO : t.getFinalScore(),
                        Comparator.reverseOrder()));
                String deptGrade = levelTables.get(0).getDeptGrade();
                // 查询单条
                GradeQuotaConfig quota = quotaMapper.selectOne(new QueryWrapper<GradeQuotaConfig>()
                        .eq("dept_grade", deptGrade)
                        .eq("staff_level", staffLevel)
                        .last("LIMIT 1"));

                // 判空处理
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

    private int calcQuota(int total, BigDecimal ratio) {

        // 判空处理
        if (ratio == null) {

            return 0;
        }

        return BigDecimal.valueOf(total).multiply(ratio)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).intValue();
    }

    private void fillGrades(List<AssessmentTable> tables, int quotaA, int quotaB, int quotaC, int quotaD,

                            String deptGrade, Long periodId) {

        int idx = 0;

        for (AssessmentTable t : tables) {

            String grade;

            // 条件分支
            if (idx < quotaA) {

                grade = "A";

            } else if (idx < quotaA + quotaB) {

                grade = "B";

            } else if (idx < quotaA + quotaB + quotaC) {

                grade = "C";

            } else {

                grade = "D";
            }

            // 更新记录
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

    // 按部门查找部门考核
    private DeptAssessment findDeptAssessment(List<DeptAssessment> list, Long deptId) {

        for (DeptAssessment d : list) {

            // 部门归属判断
            if (d.getDeptId().equals(deptId)) {

                return d;
            }
        }

        return null;
    }

    // 解析员工层级（按被考核人角色）：
     // 部门领导(DEPT_LEAD) → MIDDLE（最终分=部门分×权重+个人分×权重）；
     // 员工等其余角色 → BASIC。

    private String resolveStaffLevel(AssessmentTable t, Map<Long, String> roleByUserId) {

        // 取用户角色（已批量预加载，避免逐表查询）
        String role = t.getUserId() == null ? null : roleByUserId.get(t.getUserId());

        // 非空才处理
        if (RoleConst.ROLE_DEPT_LEAD.equals(role)) {

            return LEVEL_MIDDLE;
        }

        return LEVEL_BASIC;
    }

    // 个人最终分 = 自评总分×50% + 领导评分×50%（与考核收口 finalizeWithLeaderScore 同口径）。
    // 仅当存在领导评分时参与混算，缺失（异常数据）则跳过混算，最终分保持个人分。
    private BigDecimal personalComposite(AssessmentTable t) {

        if (t.getLeaderScore() == null) {

            return null;
        }
        BigDecimal self = t.getSelfTotalScore() == null ? BigDecimal.ZERO : t.getSelfTotalScore();

        return self.multiply(BigDecimal.valueOf(0.5))
                .add(t.getLeaderScore().multiply(BigDecimal.valueOf(0.5)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
