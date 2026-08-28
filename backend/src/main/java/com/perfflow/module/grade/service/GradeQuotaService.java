package com.perfflow.module.grade.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.grade.dto.GradeQuotaReq;
import com.perfflow.module.grade.entity.GradeQuotaConfig;
import com.perfflow.module.grade.mapper.GradeQuotaConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 等级配额配置服务（运营管理部/管理员维护）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeQuotaService {

    private final GradeQuotaConfigMapper quotaMapper;

    /**
     * 等级配额配置列表。
     *
     * @return 全部配置
     */
    public List<GradeQuotaConfig> list() {
        return quotaMapper.selectList(new QueryWrapper<GradeQuotaConfig>()
                .orderByAsc("dept_grade").orderByAsc("staff_level"));
    }

    /**
     * 新增等级配额配置。
     *
     * @param req 配置请求
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(GradeQuotaReq req) {
        Long exist = quotaMapper.selectCount(new QueryWrapper<GradeQuotaConfig>()
                .eq("dept_grade", req.getDeptGrade())
                .eq("staff_level", req.getStaffLevel()));
        if (exist != null && exist > 0) {
            throw new BizException(ResultCode.GRADE_QUOTA_EXISTS);
        }
        GradeQuotaConfig config = new GradeQuotaConfig();
        config.setDeptGrade(req.getDeptGrade());
        config.setStaffLevel(req.getStaffLevel());
        config.setGradeARatio(req.getGradeARatio());
        config.setGradeBRatio(req.getGradeBRatio());
        config.setGradeCRatio(req.getGradeCRatio());
        config.setGradeDRatio(req.getGradeDRatio());
        config.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()));
        quotaMapper.insert(config);
        log.info("新增等级配额配置: deptGrade={}, staffLevel={}", req.getDeptGrade(), req.getStaffLevel());
        return config.getId();
    }

    /**
     * 更新等级配额配置。
     *
     * @param id  配置ID
     * @param req 配置请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GradeQuotaReq req) {
        GradeQuotaConfig config = quotaMapper.selectById(id);
        if (config == null) {
            throw new BizException(ResultCode.GRADE_QUOTA_NOT_FOUND);
        }
        config.setDeptGrade(req.getDeptGrade());
        config.setStaffLevel(req.getStaffLevel());
        config.setGradeARatio(req.getGradeARatio());
        config.setGradeBRatio(req.getGradeBRatio());
        config.setGradeCRatio(req.getGradeCRatio());
        config.setGradeDRatio(req.getGradeDRatio());
        config.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()));
        quotaMapper.updateById(config);
        log.info("更新等级配额配置: id={}", id);
    }

    /**
     * 删除等级配额配置。
     *
     * @param id 配置ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GradeQuotaConfig config = quotaMapper.selectById(id);
        if (config == null) {
            throw new BizException(ResultCode.GRADE_QUOTA_NOT_FOUND);
        }
        quotaMapper.deleteById(id);
        log.info("删除等级配额配置: id={}", id);
    }
}
