package com.perfflow.module.audit.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.perfflow.module.audit.entity.AdjustLog;
import org.apache.ibatis.annotations.Mapper;
@Mapper
// 调整审计日志 Mapper。

public interface AdjustLogMapper extends BaseMapper<AdjustLog> {
}
