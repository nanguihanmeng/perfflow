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
// 调整审计日志服务。
@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustLogService {

    private final AdjustLogMapper adjustLogMapper;
    // 记录调整（前台不可见）。
    @Transactional(rollbackFor = Exception.class)
    // 执行 recordAdjust。
    public void recordAdjust(String targetType, Long targetId, String fieldName,

                             String beforeValue, String afterValue, String reason) {

        AdjustLog entity = new AdjustLog();
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setFieldName(fieldName);
        entity.setBeforeValue(beforeValue);
        entity.setAfterValue(afterValue);
        // 取当前用户上下文
        entity.setOperatorId(DataScopeContext.currentUserId());
        entity.setAdjustReason(reason);
        entity.setIsVisible(false);
        // 写入记录
        adjustLogMapper.insert(entity);
        log.debug("记录调整日志: target={}:{}, field={}, {} -> {}",
                targetType, targetId, fieldName, beforeValue, afterValue);
    }

    // 完整审计日志（运营管理部/管理员）。

    // 查询并返回结果
    public List<AdjustLog> getFullAuditLog(String targetType, Long targetId) {

        // 判空处理
        if (targetType == null || targetId == null) {

            // 空结果返回空集合
            return Collections.emptyList();
        }

        // 查询列表
        return adjustLogMapper.selectList(new QueryWrapper<AdjustLog>()
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .orderByAsc("created_at"));
    }
}
