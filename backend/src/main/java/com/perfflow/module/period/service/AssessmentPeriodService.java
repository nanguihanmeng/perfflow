package com.perfflow.module.period.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.service.AssessmentTableService;
import com.perfflow.module.deptassessment.service.DeptAssessmentService;
import com.perfflow.module.period.dto.PeriodCreateReq;
import com.perfflow.module.period.dto.PeriodResp;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.enums.PeriodType;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 考核周期业务逻辑：列表、当前周期、创建、开放、关闭。
 *
 * <p>状态机：0=未开始；1=进行中；2=已结束。
 *
 * @author PerfFlow
 */
@Service
@RequiredArgsConstructor
public class AssessmentPeriodService {

    /** 周期状态 - 未开始。 */
    private static final int STATUS_PENDING = 0;

    /** 周期状态 - 进行中。 */
    private static final int STATUS_OPEN = 1;

    /** 周期状态 - 已结束。 */
    private static final int STATUS_CLOSED = 2;

    private final AssessmentPeriodMapper periodMapper;
    private final AssessmentTableService tableService;
    private final DeptAssessmentService deptAssessmentService;

    /**
     * 查询全部周期，按年/季度倒序。
     *
     * @return 周期响应列表
     */
    public List<PeriodResp> list() {
        List<AssessmentPeriod> all = periodMapper.selectList(
                new QueryWrapper<AssessmentPeriod>().orderByDesc("year").orderByDesc("quarter"));
        List<PeriodResp> out = new ArrayList<>(all.size());
        for (AssessmentPeriod p : all) {
            out.add(PeriodResp.from(p));
        }
        return out;
    }

    /**
     * 查询当前活跃周期。
     *
     * <p>判定：start_date ≤ today 且 today 在 (suspend_end_date OR dept_review_end_date) 之内。
     *
     * @param today 业务日期，传 null 时取系统当前日期
     * @return 当前周期，未匹配返回 null
     */
    public PeriodResp current(LocalDate today) {
        LocalDate theDay = today == null ? LocalDate.now() : today;
        QueryWrapper<AssessmentPeriod> wrapper = new QueryWrapper<>();
        // 显式指定 lambda 参数类型为 QueryWrapper，避免泛型 Param 推导失败
        wrapper.and((QueryWrapper<AssessmentPeriod> qw) -> qw
                        .le("start_date", theDay)
                        .and(inner -> inner
                                .ge("suspend_end_date", theDay)
                                .or(w -> w.ge("dept_review_end_date", theDay))))
                .orderByDesc("year")
                .last("LIMIT 1");
        AssessmentPeriod p = periodMapper.selectOne(wrapper);
        return p == null ? null : PeriodResp.from(p);
    }

    /**
     * 创建考核周期。
     *
     * <p>按周期类型校验：年度类型（表3/5/7）季度强制为 0；同一 年+季度+类型 唯一。
     *
     * @param req 创建请求
     * @return 新周期ID
     * @throws BizException 校验失败或已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(PeriodCreateReq req) {
        PeriodType type = PeriodType.of(req.getPeriodType());
        Integer quarter = req.getQuarter() == null ? 0 : req.getQuarter();
        if (type.isAnnual()) {
            // 年度周期无季度概念，统一按 0 存储（唯一键 year+quarter+period_type 区分年度多表）
            quarter = 0;
        }
        Long exist = periodMapper.selectCount(new QueryWrapper<AssessmentPeriod>()
                .eq("year", req.getYear())
                .eq("quarter", quarter)
                .eq("period_type", type.name()));
        if (exist != null && exist > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "该年份该类型周期已存在");
        }
        AssessmentPeriod p = new AssessmentPeriod();
        p.setName(req.getName());
        p.setPeriodType(type.name());
        p.setYear(req.getYear());
        p.setQuarter(quarter);
        p.setStartDate(req.getStartDate());
        p.setSuspendEndDate(req.getSuspendEndDate());
        p.setDeptReviewEndDate(req.getDeptReviewEndDate());
        p.setLeadScoreEndDate(req.getLeadScoreEndDate());
        p.setAutoPushOnExpire(Boolean.TRUE.equals(req.getAutoPushOnExpire()));
        p.setStatus(STATUS_PENDING);
        periodMapper.insert(p);
        return p.getId();
    }

    /**
     * 打开周期：状态置为进行中，并按类型生成考核表。
     *
     * <p>个人线（表1-4）：为勾选的被考核人生成个人考核主表 + 10 行模板；
     * 部门线（表5-7）：为勾选的部门生成部门考核主表 + KPI 行模板。
     *
     * @param id      周期ID
     * @param userIds 参与考核的员工 ID 列表（个人线，为空表示全员）
     * @param deptIds 参与考核的部门 ID 列表（部门线，为空表示全部部门）
     * @throws BizException 周期不存在或已结束时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void open(Long id, List<Long> userIds, List<Long> deptIds) {
        AssessmentPeriod p = required(id);
        if (Integer.valueOf(STATUS_CLOSED).equals(p.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "周期已结束");
        }
        p.setStatus(STATUS_OPEN);
        periodMapper.updateById(p);
        PeriodType type = PeriodType.of(p.getPeriodType());
        if (type.isDeptLine()) {
            deptAssessmentService.initForPeriod(p, deptIds);
        } else {
            tableService.initForPeriod(p, userIds);
        }
    }

    /**
     * 关闭周期。
     *
     * @param id 周期ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        AssessmentPeriod p = required(id);
        p.setStatus(STATUS_CLOSED);
        periodMapper.updateById(p);
    }

    /**
     * 根据ID获取周期，不存在时抛出业务异常。
     *
     * @param id 周期ID
     * @return 周期实体
     * @throws BizException 不存在时抛出
     */
    public AssessmentPeriod required(Long id) {
        AssessmentPeriod p = periodMapper.selectById(id);
        if (p == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return p;
    }
}
