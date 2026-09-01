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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//
 // 考核进度统计服务（看板数据）。
 //

 //

 //
@Service
@RequiredArgsConstructor
public class ProgressService {

    //
    private static final long OVERDUE_DAYS = 3L;
    private final AssessmentTableMapper tableMapper;
    private final SysDepartmentMapper deptMapper;
    //
     // 全公司进度看板。
     //

     //
    public ProgressResp dashboard() {
        // 查询列表
        List<AssessmentTable> tables = tableMapper.selectList(
                new QueryWrapper<AssessmentTable>().orderByAsc("dept_id"));
        // 查询列表
        List<SysDepartment> depts = deptMapper.selectList(new QueryWrapper<SysDepartment>());
        // 按部门分组累计统计，单次遍历完成
        Map<Long, DeptCounter> counterByDept = new HashMap<>(depts.size() * 2);

        for (AssessmentTable t : tables) {

            Long deptId = t.getDeptId();
            DeptCounter counter = counterByDept.computeIfAbsent(deptId, k -> new DeptCounter());
            counter.total++;
            AssessmentState state = AssessmentState.valueOf(t.getState());
            // 非自评中即视为已填报
            if (state != AssessmentState.SELF_DRAFTING) {

                counter.filled++;
            }
            // 进入审核链路即视为已审
            if (state == AssessmentState.DEPT_REVIEW || state == AssessmentState.LEAD_SCORING

                    || state == AssessmentState.FINISHED) {

                counter.reviewed++;
            }
            // 挂起超期视为逾期
            if (state == AssessmentState.SELF_SUSPENDED && isOverdue(t)) {

                counter.overdue++;
            }
        }

        ProgressResp resp = new ProgressResp();
        long total = 0;
        long filled = 0;
        long reviewed = 0;
        long overdue = 0;
        // 构建集合容器
        List<DeptProgressResp> details = new ArrayList<>(depts.size());

        for (SysDepartment dept : depts) {

            DeptCounter counter = counterByDept.getOrDefault(dept.getId(), new DeptCounter());
            total += counter.total;
            filled += counter.filled;
            reviewed += counter.reviewed;
            overdue += counter.overdue;
            DeptProgressResp detail = new DeptProgressResp();
            detail.setDeptId(dept.getId());
            detail.setDeptName(dept.getName());
            detail.setTotal(counter.total);
            detail.setFilled(counter.filled);
            detail.setUnfilled(counter.total - counter.filled);
            detail.setReviewed(counter.reviewed);
            detail.setOverdue(counter.overdue);
            detail.setCompletionRate(counter.total == 0 ? 0.0
                    : Math.round(counter.filled * 1000.0 / counter.total) / 10.0);
            details.add(detail);
        }

        resp.setTotal(total);
        resp.setFilled(filled);
        resp.setUnfilled(total - filled);
        resp.setReviewed(reviewed);
        resp.setOverdue(overdue);
        resp.setDeptDetails(details);
        // 返回结果
        return resp;
    }

    //
     // 是否逾期：挂起状态且提交时间距今超过逾期天数。
     //

     //
    private boolean isOverdue(AssessmentTable t) {

        // 判空处理
        if (t.getSubmittedAt() == null) {

            return false;
        }

        Duration duration = Duration.between(t.getSubmittedAt(), LocalDateTime.now());
        return duration.toDays() > OVERDUE_DAYS;
    }

    //
    private static class DeptCounter {
        long total;
        long filled;
        long reviewed;
        long overdue;
    }
}
