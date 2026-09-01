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
// 个人考核流程日志服务。
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentFlowService {

    private final AssessmentFlowLogMapper logMapper;
    private final SysUserMapper userMapper;
    // 写入流程日志，无批注。
    @Transactional
    // 写入流程日志。
    public void writeLog(AssessmentTable t,
                         AssessmentState from,
                         AssessmentState to,

                         String action) {

        writeLog(t, from, to, action, null);
    }

    // 写入流程日志，含批注。
    @Transactional
    // 写入流程日志。
    public void writeLog(AssessmentTable t,
                         AssessmentState from,
                         AssessmentState to,
                         String action,

                         String comment) {

        // 取当前用户上下文
        Long opId = DataScopeContext.currentUserId();
        // 取当前用户上下文
        String role = DataScopeContext.current().getPrimaryRole();
        AssessmentFlowLog entity = new AssessmentFlowLog();
        entity.setTableId(t.getId());
        entity.setFromState(from.name());
        entity.setToState(to.name());
        entity.setAction(action);
        entity.setOperatorId(opId);
        entity.setOperatorRole(role);
        entity.setComment(comment);
        // 写入记录
        logMapper.insert(entity);
        log.debug("流程日志: tableId={}, {} -> {}, action={}, operator={}({})",
                t.getId(), from, to, action, opId, role);
    }

    // 查询流程日志
    public List<FlowLogResp> listLogs(Long tableId) {

        // 查询列表
        List<AssessmentFlowLog> logs = logMapper.selectList(
                new QueryWrapper<AssessmentFlowLog>().eq("table_id", tableId).orderByAsc("created_at"));
        // 构建集合容器
        List<FlowLogResp> out = new ArrayList<>();
        // 批量收集操作者ID，一次查询避免 N+1
        List<Long> opIds = logs.stream().map(AssessmentFlowLog::getOperatorId)
                // 集合流处理
                .filter(java.util.Objects::nonNull).distinct().toList();
        // 构建集合容器
        Map<Long, String> nameCache = new HashMap<>();

        // 条件分支
        if (!opIds.isEmpty()) {

            // 系统数据访问
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

            // 非空才处理
            if (l.getOperatorId() != null) {

                r.setOperatorName(nameCache.get(l.getOperatorId()));
            }

            out.add(r);
        }

        // 返回结果
        return out;
    }
}
