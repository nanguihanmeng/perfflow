package com.perfflow.module.deptassessment.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.module.deptassessment.dto.DeptFlowLogResp;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.entity.DeptAssessmentFlowLog;
import com.perfflow.module.deptassessment.enums.DeptAssessmentState;
import com.perfflow.module.deptassessment.mapper.DeptAssessmentFlowLogMapper;
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
import java.util.Objects;
/**
 * 部门考核流程日志服务：记录各流转动作的操作人与操作内容，供审计留痕与前端展示流转轨迹。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptAssessmentFlowService {

    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_REVIEW_PASS = "REVIEW_PASS";
    public static final String ACTION_REVIEW_REJECT = "REVIEW_REJECT";
    public static final String ACTION_AUDIT_PASS = "AUDIT_PASS";
    public static final String ACTION_AUDIT_REJECT = "AUDIT_REJECT";
    public static final String ACTION_APPROVE = "APPROVE";
    private final DeptAssessmentFlowLogMapper logMapper;
    private final SysUserMapper userMapper;
    /**
     * 写入一条部门考核流程日志，操作人与操作角色取自当前请求上下文。
     * @param assessment 部门考核主表
     * @param from 流转前状态
     * @param to 流转后状态
     * @param action 动作标识（SUBMIT / REVIEW_PASS 等）
     * @param comment 处理说明或退回原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void writeLog(DeptAssessment assessment, DeptAssessmentState from,

                         DeptAssessmentState to, String action, String comment) {

        // 取当前用户上下文
        Long opId = DataScopeContext.currentUserId();
        // 取当前用户上下文
        String role = DataScopeContext.current() == null ? null : DataScopeContext.current().getPrimaryRole();
        DeptAssessmentFlowLog entity = new DeptAssessmentFlowLog();
        entity.setAssessmentId(assessment.getId());
        entity.setFromStatus(from.getCode());
        entity.setToStatus(to.getCode());
        entity.setAction(action);
        entity.setOperatorId(opId);
        entity.setOperatorRole(role);
        entity.setComment(comment);
        // 写入记录
        logMapper.insert(entity);
        log.debug("部门考核流程日志: assessmentId={}, {} -> {}, action={}, operator={}({})",
                assessment.getId(), from, to, action, opId, role);
    }

    /**
     * 查询某部门考核的流程日志（按时间正序），并回填经办人真实姓名。
     * @param assessmentId 部门考核主表 ID
     */
    public List<DeptFlowLogResp> listLogs(Long assessmentId) {

        // 查询列表
        List<DeptAssessmentFlowLog> logs = logMapper.selectList(
                new QueryWrapper<DeptAssessmentFlowLog>()
                        .eq("assessment_id", assessmentId)
                        .orderByAsc("created_at"));
        // 构建集合容器
        List<DeptFlowLogResp> out = new ArrayList<>(logs.size());
        // 批量收集经办人ID，一次查询避免 N+1
        List<Long> opIds = logs.stream().map(DeptAssessmentFlowLog::getOperatorId)
                // 集合流处理
                .filter(Objects::nonNull).distinct().toList();
        // 构建集合容器
        Map<Long, String> nameCache = new HashMap<>();

        // 条件分支
        if (!opIds.isEmpty()) {

            // 系统数据访问
            for (SysUser u : userMapper.selectBatchIds(opIds)) {

                nameCache.put(u.getId(), u.getRealName());
            }
        }

        for (DeptAssessmentFlowLog l : logs) {

            DeptFlowLogResp r = new DeptFlowLogResp();
            r.setId(l.getId());
            r.setAssessmentId(l.getAssessmentId());
            r.setFromStatus(l.getFromStatus());
            r.setToStatus(l.getToStatus());
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
