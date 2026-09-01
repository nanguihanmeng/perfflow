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
import com.perfflow.task.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// 考核周期业务逻辑：列表、当前周期、创建、开放、关闭。
@Service
@RequiredArgsConstructor
public class AssessmentPeriodService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_OPEN = 1;
    private static final int STATUS_CLOSED = 2;
    private final AssessmentPeriodMapper periodMapper;
    private final AssessmentTableService tableService;
    private final DeptAssessmentService deptAssessmentService;
    private final AsyncTaskService asyncTaskService;

    // 查询列表数据
    public List<PeriodResp> list() {

        // 查询列表
        List<AssessmentPeriod> all = periodMapper.selectList(
                new QueryWrapper<AssessmentPeriod>().orderByDesc("year").orderByDesc("quarter"));
        // 构建集合容器
        List<PeriodResp> out = new ArrayList<>(all.size());

        for (AssessmentPeriod p : all) {

            out.add(PeriodResp.from(p));
        }

        // 返回结果
        return out;
    }

    // 执行业务处理
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
        // 查询单条
        AssessmentPeriod p = periodMapper.selectOne(wrapper);
        return p == null ? null : PeriodResp.from(p);
    }

    @Transactional(rollbackFor = Exception.class)

    // 创建记录
    public Long create(PeriodCreateReq req) {

        PeriodType type = PeriodType.of(req.getPeriodType());
        Integer quarter = req.getQuarter() == null ? 0 : req.getQuarter();

        // 条件分支
        if (type.isAnnual()) {
            // 年度周期无季度概念，统一按 0 存储（唯一键 year+quarter+period_type 区分年度多表）
            quarter = 0;
        }

        // 统计数量
        Long exist = periodMapper.selectCount(new QueryWrapper<AssessmentPeriod>()
                .eq("year", req.getYear())
                .eq("quarter", quarter)
                .eq("period_type", type.name()));

        // 非空才处理
        if (exist != null && exist > 0) {

            // 校验失败抛异常
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
        // 写入记录
        periodMapper.insert(p);
        return p.getId();
    }

    // 打开周期：状态置为进行中，并按类型生成考核表。
     // 部门线（表5-7）：为勾选的部门生成部门考核主表 + KPI 行模板。
    @Transactional(rollbackFor = Exception.class)

    // 执行业务处理
    public void open(Long id, List<Long> userIds, List<Long> deptIds) {

        // 加载实体并校验存在
        AssessmentPeriod p = required(id);

        // 状态判断
        if (Integer.valueOf(STATUS_CLOSED).equals(p.getStatus())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "周期已结束");
        }

        p.setStatus(STATUS_OPEN);
        // 更新记录
        periodMapper.updateById(p);
        PeriodType type = PeriodType.of(p.getPeriodType());
        // 部门线同步建表；个人线在事务提交后异步建表，避免阻塞开启请求
        if (type.isDeptLine()) {

            // 调用业务服务
            deptAssessmentService.initForPeriod(p, deptIds);

        } else {

            // 注册异步建表
            registerAsyncInit(p, userIds);
        }
    }

    // 注册事务提交后的异步建表
    private void registerAsyncInit(AssessmentPeriod p, List<Long> userIds) {

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {

                // 异步执行
                asyncTaskService.initPersonalTablesAsync(p, userIds);
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)

    // 执行业务处理
    public void close(Long id) {

        // 加载实体并校验存在
        AssessmentPeriod p = required(id);
        p.setStatus(STATUS_CLOSED);
        // 更新记录
        periodMapper.updateById(p);
    }

    // 根据ID获取周期，不存在时抛出业务异常。

    // 执行业务处理
    public AssessmentPeriod required(Long id) {

        // 查询单条
        AssessmentPeriod p = periodMapper.selectById(id);

        // 判空处理
        if (p == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.NOT_FOUND);
        }

        return p;
    }
}
