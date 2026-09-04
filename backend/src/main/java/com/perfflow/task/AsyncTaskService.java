package com.perfflow.task;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.module.assessment.service.AssessmentTableService;
import com.perfflow.module.notification.service.NotificationService;
import com.perfflow.module.period.entity.AssessmentPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;
//
 // 异步任务服务：承载周期开启后的批量建表等耗时操作。
 //
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final AssessmentTableService tableService;
    private final NotificationService notificationService;
    //
     // 异步为个人线周期批量生成考核主表，完成后通知 HR。
     //

     //
    @Async("asyncExecutor")
    public void initPersonalTablesAsync(AssessmentPeriod period, List<Long> userIds) {
        try {

            tableService.initForPeriod(period, userIds);
            // 发送站内通知给绩效考核管理员
            notificationService.sendToRole(RoleConst.ROLE_PERFORMANCE_HR, "周期开启完成",
                    "周期 " + period.getName() + " 的考核表已全部生成", "SYSTEM");

        } catch (Exception e) {

            log.error("异步生成个人考核表失败: periodId={}", period.getId(), e);
            // 发送站内通知给绩效考核管理员
            notificationService.sendToRole(RoleConst.ROLE_PERFORMANCE_HR, "周期开启失败",
                    "周期 " + period.getName() + " 的考核表生成失败，请检查数据", "SYSTEM");
        }
    }
}
