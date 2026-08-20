package com.perfflow.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.assessment.service.AssessmentFlowService;
import com.perfflow.module.assessment.service.AssessmentTableService;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 定时任务：
 * <ul>
 *   <li>每天 08:00 检查挂起即将结束（<=3 天）并打印提醒（已通过 /home/reminders 实时计算）</li>
 *   <li>每天 23:00 对超期未推送且 {@code auto_push_on_expire=1} 的主表自动 PUSH</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingScheduleTask {

    private final AssessmentPeriodMapper periodMapper;
    private final AssessmentTableMapper tableMapper;
    private final AssessmentTableService tableService;
    private final AssessmentFlowService flowService;

    @Scheduled(cron = "${perfflow.scheduler.pending-reminder-cron:0 0 8 * * ?}")
    public void checkSuspendPending() {
        LocalDate today = LocalDate.now();
        List<AssessmentPeriod> periods = periodMapper.selectList(
                new QueryWrapper<AssessmentPeriod>().eq("status", 1));
        for (AssessmentPeriod p : periods) {
            long days = ChronoUnit.DAYS.between(today, p.getSuspendEndDate());
            if (days >= 0 && days <= 3) {
                log.info("[PerfFlow] 周期 {} 自评挂起将在 {} 天后结束", p.getName(), days);
            }
        }
    }

    @Scheduled(cron = "0 0 23 * * ?")
    @Transactional
    public void autoPushExpired() {
        LocalDate today = LocalDate.now();
        // 自动推送
        List<AssessmentPeriod> periods = periodMapper.selectList(
                new QueryWrapper<AssessmentPeriod>().eq("status", 1)
                        .eq("auto_push_on_expire", true));
        for (AssessmentPeriod p : periods) {
            LocalDate effective = p.getSuspendEndDate(); // 已含延长天数（本版本不依赖延期后实际日期，简化由人事手动延）
            if (effective == null || today.isBefore(effective)) continue;
            List<AssessmentTable> tables = tableMapper.selectList(
                    new QueryWrapper<AssessmentTable>()
                            .eq("period_id", p.getId())
                            .eq("state", AssessmentState.SELF_SUSPENDED.name()));
            for (AssessmentTable t : tables) {
                try {
                    tableService.push(t.getId());
                    flowService.writeLog(t, AssessmentState.SELF_SUSPENDED, AssessmentState.DEPT_REVIEW,
                            "AUTO_PUSH", "挂起到期自动推送");
                } catch (Exception e) {
                    log.warn("auto push failed tableId={}: {}", t.getId(), e.getMessage());
                }
            }
        }
    }
}
