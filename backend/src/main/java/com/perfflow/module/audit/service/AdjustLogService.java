package com.perfflow.module.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.module.audit.entity.AdjustLog;
import com.perfflow.module.audit.mapper.AdjustLogMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 调整审计日志服务。
 *
 * <p>记录调整（前台不可见，is_visible=false）；完整审计日志仅运营管理部/管理员可见。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustLogService {

    private final AdjustLogMapper adjustLogMapper;

    /**
     * 记录调整（前台不可见）。
     *
     * @param targetType 目标类型 DEPT/PERSONAL
     * @param targetId   目标ID
     * @param fieldName  变更字段
     * @param beforeValue 变更前值
     * @param afterValue  变更后值
     * @param reason      调整原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordAdjust(String targetType, Long targetId, String fieldName,
                             String beforeValue, String afterValue, String reason) {
        AdjustLog entity = new AdjustLog();
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setFieldName(fieldName);
        entity.setBeforeValue(beforeValue);
        entity.setAfterValue(afterValue);
        entity.setOperatorId(DataScopeContext.currentUserId());
        entity.setAdjustReason(reason);
        entity.setIsVisible(false);
        adjustLogMapper.insert(entity);
        log.debug("记录调整日志: target={}:{}, field={}, {} -> {}",
                targetType, targetId, fieldName, beforeValue, afterValue);
    }

    /**
     * 完整审计日志（运营管理部/管理员）。
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 审计日志列表
     */
    public List<AdjustLog> getFullAuditLog(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return Collections.emptyList();
        }
        return adjustLogMapper.selectList(new QueryWrapper<AdjustLog>()
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .orderByAsc("created_at"));
    }
}
