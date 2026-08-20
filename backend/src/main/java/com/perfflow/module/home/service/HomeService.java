package com.perfflow.module.home.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.home.dto.RemindersResp;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final AssessmentTableMapper tableMapper;
    private final AssessmentPeriodMapper periodMapper;

    public RemindersResp load() {
        RemindersResp out = new RemindersResp();
        String role = DataScopeContext.current().getPrimaryRole();
        Long currentUid = DataScopeContext.currentUserId();
        Long deptId = DataScopeContext.currentDeptId();

        // ---- 1) 我的待办：根据当前角色筛选 ----
        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        switch (role == null ? "" : role) {
            case RoleConst.ROLE_EMP -> {
                if (currentUid != null) qw.eq("user_id", currentUid);
                qw.in("state",
                        AssessmentState.SELF_DRAFTING.name(),
                        AssessmentState.SELF_SUSPENDED.name(),
                        AssessmentState.LEAD_SCORING.name());
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                if (deptId != null) qw.eq("dept_id", deptId);
                qw.in("state", AssessmentState.DEPT_REVIEW.name());
            }
            case RoleConst.ROLE_LEAD -> {
                qw.in("state", AssessmentState.LEAD_SCORING.name());
            }
            case RoleConst.ROLE_HR -> {
                qw.in("state", AssessmentState.SELF_SUSPENDED.name());
            }
            default -> { /* ADMIN 已拦截 */ }
        }
        qw.orderByAsc("id");
        List<AssessmentTable> todos = tableMapper.selectList(qw);
        for (AssessmentTable t : todos) {
            RemindersResp.Reminder r = new RemindersResp.Reminder();
            r.setType(t.getState());
            r.setTitle(stateText(t.getState()) + " #" + t.getId());
            r.setDescription("考核主表 #" + t.getId() + " 等待处理");
            r.setTargetTableId(t.getId());
            r.setSeverity("SELF_SUSPENDED".equals(t.getState()) ? 2 : 1);
            out.getTodos().add(r);
        }

        // ---- 2) 挂起结束前 3 天的提醒（HR/部门领导可见） ----
        LocalDate today = LocalDate.now();
        List<AssessmentPeriod> periods = periodMapper.selectList(new QueryWrapper<AssessmentPeriod>());
        for (AssessmentPeriod p : periods) {
            if (p.getSuspendEndDate() == null) continue;
            long days = ChronoUnit.DAYS.between(today, p.getSuspendEndDate());
            if (days >= 0 && days <= 3) {
                RemindersResp.Reminder r = new RemindersResp.Reminder();
                r.setType("SUSPEND_SOON");
                r.setTitle("周期 " + p.getName() + " 自评挂起将在 " + days + " 天后结束");
                r.setDescription("请及时确认推送或延长挂起时间");
                r.setSeverity(days <= 1 ? 3 : 2);
                out.getUpcomingSuspends().add(r);
            }
        }
        return out;
    }

    private String stateText(String s) {
        return switch (s) {
            case "SELF_DRAFTING" -> "请继续填报自评";
            case "SELF_SUSPENDED" -> "等待人事确认推送";
            case "DEPT_REVIEW" -> "请部门领导审核";
            case "LEAD_SCORING" -> "领导评分中";
            case "FINISHED" -> "已完成";
            default -> s;
        };
    }
}
