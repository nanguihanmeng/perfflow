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
import java.math.BigDecimal;
import java.util.List;
// 权重配置服务（管理员维护）。
@Slf4j
@Service
@RequiredArgsConstructor
public class WeightConfigService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
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

        validateWeights(req);
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

        validateWeights(req);

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

    // 校验权重：部门权重与个人权重均在 0-100 之间，且二者之和为 100。
    private static void validateWeights(WeightConfigReq req) {
        if (req.getDeptWeight() == null || req.getPersonalWeight() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "权重不能为空");
        }
        if (req.getDeptWeight().compareTo(BigDecimal.ZERO) < 0 || req.getDeptWeight().compareTo(HUNDRED) > 0
                || req.getPersonalWeight().compareTo(BigDecimal.ZERO) < 0
                || req.getPersonalWeight().compareTo(HUNDRED) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "权重需在 0-100 之间");
        }
        if (req.getDeptWeight().add(req.getPersonalWeight()).compareTo(HUNDRED) != 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "部门权重与个人权重之和须等于 100");
        }
    }
}
