package com.perfflow.module.grade.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.grade.dto.WeightConfigReq;
import com.perfflow.module.grade.entity.WeightConfig;
import com.perfflow.module.grade.mapper.WeightConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
// 权重配置服务（管理员维护）。
@Slf4j
@Service
@RequiredArgsConstructor
public class WeightConfigService {

    private final WeightConfigMapper weightMapper;
    // 权重配置列表。

    // 查询列表数据
    public List<WeightConfig> list() {

        // 查询列表
        return weightMapper.selectList(new QueryWrapper<WeightConfig>().orderByAsc("staff_level"));
    }

    // 新增权重配置。
    @Transactional(rollbackFor = Exception.class)

    // 创建记录
    public Long create(WeightConfigReq req) {

        WeightConfig config = new WeightConfig();
        config.setStaffLevel(req.getStaffLevel());
        config.setDeptWeight(req.getDeptWeight());
        config.setPersonalWeight(req.getPersonalWeight());
        config.setPeriodId(req.getPeriodId());
        // 写入记录
        weightMapper.insert(config);
        log.info("新增权重配置: staffLevel={}, deptWeight={}, personalWeight={}",
                req.getStaffLevel(), req.getDeptWeight(), req.getPersonalWeight());
        return config.getId();
    }

    @Transactional(rollbackFor = Exception.class)

    // 更新记录
    public void update(Long id, WeightConfigReq req) {

        // 查询单条
        WeightConfig config = weightMapper.selectById(id);

        // 判空处理
        if (config == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.WEIGHT_CONFIG_NOT_FOUND);
        }

        config.setStaffLevel(req.getStaffLevel());
        config.setDeptWeight(req.getDeptWeight());
        config.setPersonalWeight(req.getPersonalWeight());
        config.setPeriodId(req.getPeriodId());
        // 更新记录
        weightMapper.updateById(config);
        log.info("更新权重配置: id={}", id);
    }
}
