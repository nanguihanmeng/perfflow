package com.perfflow.module.assessment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.module.assessment.dto.FlowLogResp;
import com.perfflow.module.assessment.entity.AssessmentFlowLog;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentFlowLogMapper;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentFlowService {

    private final AssessmentFlowLogMapper logMapper;
    private final SysUserMapper userMapper;

    @Transactional
    public void writeLog(AssessmentTable t,
                         AssessmentState from,
                         AssessmentState to,
                         String action) {
        writeLog(t, from, to, action, null);
    }

    @Transactional
    public void writeLog(AssessmentTable t,
                         AssessmentState from,
                         AssessmentState to,
                         String action,
                         String comment) {
        Long opId = DataScopeContext.currentUserId();
        String role = DataScopeContext.current().getPrimaryRole();
        AssessmentFlowLog log = new AssessmentFlowLog();
        log.setTableId(t.getId());
        log.setFromState(from.name());
        log.setToState(to.name());
        log.setAction(action);
        log.setOperatorId(opId);
        log.setOperatorRole(role);
        log.setComment(comment);
        logMapper.insert(log);
    }

    public List<FlowLogResp> listLogs(Long tableId) {
        List<AssessmentFlowLog> logs = logMapper.selectList(
                new QueryWrapper<AssessmentFlowLog>().eq("table_id", tableId).orderByAsc("created_at"));
        List<FlowLogResp> out = new ArrayList<>();
        for (AssessmentFlowLog l : logs) {
            FlowLogResp r = new FlowLogResp();
            r.setId(l.getId());
            r.setFromState(l.getFromState());
            r.setToState(l.getToState());
            r.setAction(l.getAction());
            r.setOperatorId(l.getOperatorId());
            r.setOperatorRole(l.getOperatorRole());
            r.setComment(l.getComment());
            r.setCreatedAt(l.getCreatedAt());
            if (l.getOperatorId() != null) {
                SysUser u = userMapper.selectById(l.getOperatorId());
                if (u != null) r.setOperatorName(u.getRealName());
            }
            out.add(r);
        }
        return out;
    }
}
