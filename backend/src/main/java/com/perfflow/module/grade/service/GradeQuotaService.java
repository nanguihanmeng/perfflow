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
// 等级配额配置服务（运营管理部/管理员维护）。
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeQuotaService {

    private final GradeQuotaConfigMapper quotaMapper;
    // 等级配额配置列表。

    // 查询列表数据
    public List<GradeQuotaConfig> list() {

        // 查询列表
        return quotaMapper.selectList(new QueryWrapper<GradeQuotaConfig>()
                .orderByAsc("dept_grade").orderByAsc("staff_level"));
    }

    // 新增等级配额配置。
    @Transactional(rollbackFor = Exception.class)

    // 创建记录
    public Long create(GradeQuotaReq req) {

        // 统计数量
        Long exist = quotaMapper.selectCount(new QueryWrapper<GradeQuotaConfig>()
                .eq("dept_grade", req.getDeptGrade())
                .eq("staff_level", req.getStaffLevel()));

        // 非空才处理
        if (exist != null && exist > 0) {

            // 校验失败抛异常
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
        // 写入记录
        quotaMapper.insert(config);
        log.info("新增等级配额配置: deptGrade={}, staffLevel={}", req.getDeptGrade(), req.getStaffLevel());
        return config.getId();
    }

    @Transactional(rollbackFor = Exception.class)

    // 更新记录
    public void update(Long id, GradeQuotaReq req) {

        // 查询单条
        GradeQuotaConfig config = quotaMapper.selectById(id);

        // 判空处理
        if (config == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.GRADE_QUOTA_NOT_FOUND);
        }

        config.setDeptGrade(req.getDeptGrade());
        config.setStaffLevel(req.getStaffLevel());
        config.setGradeARatio(req.getGradeARatio());
        config.setGradeBRatio(req.getGradeBRatio());
        config.setGradeCRatio(req.getGradeCRatio());
        config.setGradeDRatio(req.getGradeDRatio());
        config.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()));
        // 更新记录
        quotaMapper.updateById(config);
        log.info("更新等级配额配置: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)

    // 删除记录并清理关联数据
    public void delete(Long id) {

        // 查询单条
        GradeQuotaConfig config = quotaMapper.selectById(id);

        // 判空处理
        if (config == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.GRADE_QUOTA_NOT_FOUND);
        }
        // 删除记录
        quotaMapper.deleteById(id);
        log.info("删除等级配额配置: id={}", id);
    }
}
