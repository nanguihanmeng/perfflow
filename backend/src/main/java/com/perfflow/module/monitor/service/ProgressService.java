package com.perfflow.module.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.monitor.dto.DeptProgressResp;
import com.perfflow.module.monitor.dto.ProgressResp;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 考核进度统计服务（看板数据）。
 *
 * <p>统计口径：
 * <ul>
 *   <li>已填报：状态已离开 SELF_DRAFTING（即非自评中）</li>
 *   <li>未填报：SELF_DRAFTING</li>
 *   <li>已审：状态 ≥ DEPT_REVIEW</li>
 *   <li>逾期：SELF_SUSPENDED 且已超过周期 suspend_end_date</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final AssessmentTableMapper tableMapper;
    private final SysDepartmentMapper deptMapper;

    /**
     * 全公司进度看板。
     *
     * @return 看板数据
     */
    public ProgressResp dashboard() {
        List<AssessmentTable> tables = tableMapper.selectList(
                new QueryWrapper<AssessmentTable>().orderByAsc("dept_id"));
        List<SysDepartment> depts = deptMapper.selectList(new QueryWrapper<SysDepartment>());

        ProgressResp resp = new ProgressResp();
        long total = 0;
        long filled = 0;
        long reviewed = 0;
        long overdue = 0;

        List<DeptProgressResp> details = new ArrayList<>(depts.size());
        for (SysDepartment dept : depts) {
            long deptTotal = 0;
            long deptFilled = 0;
            long deptReviewed = 0;
            long deptOverdue = 0;
            for (AssessmentTable t : tables) {
                if (!dept.getId().equals(t.getDeptId())) {
                    continue;
                }
                deptTotal++;
                AssessmentState state = AssessmentState.valueOf(t.getState());
                if (state != AssessmentState.SELF_DRAFTING) {
                    deptFilled++;
                }
                if (state == AssessmentState.DEPT_REVIEW || state == AssessmentState.LEAD_SCORING
                        || state == AssessmentState.FINISHED) {
                    deptReviewed++;
                }
                if (state == AssessmentState.SELF_SUSPENDED && isOverdue(t)) {
                    deptOverdue++;
                }
            }
            total += deptTotal;
            filled += deptFilled;
            reviewed += deptReviewed;
            overdue += deptOverdue;

            DeptProgressResp detail = new DeptProgressResp();
            detail.setDeptId(dept.getId());
            detail.setDeptName(dept.getName());
            detail.setTotal(deptTotal);
            detail.setFilled(deptFilled);
            detail.setUnfilled(deptTotal - deptFilled);
            detail.setReviewed(deptReviewed);
            detail.setOverdue(deptOverdue);
            detail.setCompletionRate(deptTotal == 0 ? 0.0 : Math.round(deptFilled * 1000.0 / deptTotal) / 10.0);
            details.add(detail);
        }

        resp.setTotal(total);
        resp.setFilled(filled);
        resp.setUnfilled(total - filled);
        resp.setReviewed(reviewed);
        resp.setOverdue(overdue);
        resp.setDeptDetails(details);
        return resp;
    }

    /** 是否逾期：挂起状态且当前时间超过提交后应推送的期限（简化：挂起超 3 天视为逾期） */
    private boolean isOverdue(AssessmentTable t) {
        if (t.getSubmittedAt() == null) {
            return false;
        }
        java.time.Duration duration = java.time.Duration.between(t.getSubmittedAt(), java.time.LocalDateTime.now());
        return duration.toDays() > 3;
    }
}
