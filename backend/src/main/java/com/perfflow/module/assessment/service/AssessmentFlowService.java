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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
        AssessmentFlowLog entity = new AssessmentFlowLog();
        entity.setTableId(t.getId());
        entity.setFromState(from.name());
        entity.setToState(to.name());
        entity.setAction(action);
        entity.setOperatorId(opId);
        entity.setOperatorRole(role);
        entity.setComment(comment);
        logMapper.insert(entity);
        log.debug("流程日志: tableId={}, {} -> {}, action={}, operator={}({})",
                t.getId(), from, to, action, opId, role);
    }

    public List<FlowLogResp> listLogs(Long tableId) {
        List<AssessmentFlowLog> logs = logMapper.selectList(
                new QueryWrapper<AssessmentFlowLog>().eq("table_id", tableId).orderByAsc("created_at"));
        List<FlowLogResp> out = new ArrayList<>();
        // 批量收集操作者ID，一次查询避免 N+1
        List<Long> opIds = logs.stream().map(AssessmentFlowLog::getOperatorId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> nameCache = new HashMap<>();
        if (!opIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(opIds)) {
                nameCache.put(u.getId(), u.getRealName());
            }
        }
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
                r.setOperatorName(nameCache.get(l.getOperatorId()));
            }
            out.add(r);
        }
        return out;
    }
}
