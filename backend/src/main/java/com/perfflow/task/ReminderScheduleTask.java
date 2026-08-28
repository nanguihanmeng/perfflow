package com.perfflow.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.notification.service.NotificationService;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 催办定时任务：每天 09:00 执行。
 * <ul>
 *   <li>截止前 3 天仍未提交 → 每日推送未提交员工</li>
 *   <li>逾期未提交 → 推送员工 + 部门负责人</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduleTask {

    private final AssessmentPeriodMapper periodMapper;
    private final AssessmentTableMapper tableMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "${perfflow.scheduler.reminder-cron:0 0 9 * * ?}")
    public void remindUnsubmitted() {
        LocalDate today = LocalDate.now();
        List<AssessmentPeriod> periods = periodMapper.selectList(
                new QueryWrapper<AssessmentPeriod>().eq("status", 1));
        for (AssessmentPeriod p : periods) {
            long days = ChronoUnit.DAYS.between(today, p.getSuspendEndDate());
            if (days < 0 || days > 3) {
                continue;
            }
            List<AssessmentTable> tables = tableMapper.selectList(
                    new QueryWrapper<AssessmentTable>()
                            .eq("period_id", p.getId())
                            .eq("state", AssessmentState.SELF_DRAFTING.name()));
            for (AssessmentTable t : tables) {
                SysUser user = userMapper.selectById(t.getUserId());
                if (user == null) {
                    continue;
                }
                boolean overdue = days < 0;
                String content = overdue
                        ? "周期「" + p.getName() + "」考核已逾期，请立即完成填报"
                        : "周期「" + p.getName() + "」考核将在 " + days + " 天后截止，请尽快填报";
                notificationService.send(user.getId(), overdue ? "考核逾期提醒" : "考核填报提醒",
                        content, overdue ? "OVERDUE" : "REMIND");
                if (overdue) {
                    notifyDeptLead(t, p);
                }
            }
        }
        log.info("催办任务执行完成: 周期数={}", periods.size());
    }

    /** 逾期时抄送部门负责人 */
    private void notifyDeptLead(AssessmentTable t, AssessmentPeriod p) {
        if (t.getDeptId() == null) {
            return;
        }
        List<SysUser> leads = userMapper.selectList(new QueryWrapper<SysUser>()
                .eq("dept_id", t.getDeptId())
                .eq("role", "DEPT_LEAD")
                .eq("status", 1)
                .last("LIMIT 1"));
        for (SysUser lead : leads) {
            notificationService.send(lead.getId(), "部门考核逾期提醒",
                    "部门有员工在周期「" + p.getName() + "」中逾期未提交考核", "OVERDUE");
        }
    }
}
