package com.perfflow.module.assessment.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.dto.RowReq;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.enums.RowCategory;
import com.perfflow.module.assessment.mapper.AssessmentRowMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
// 考核行服务。
 // 行结果（考核结果）只与总分相关，行级不落库。
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentRowService {

    private final AssessmentRowMapper rowMapper;
    private final AssessmentTableService tableService;
    private final AssessmentPermissionService perm;
    private final AssessmentCalcService calcService;

     // 每次更新后即时重算整表自评总分。
    @Transactional

    // 更新记录
    public void update(Long tableId, Long rowId, RowReq req) {

        // 调用业务服务
        AssessmentTable t = tableService.getRequired(tableId);
        AssessmentRow r = getRow(rowId);

        // 数据权限处理
        if (!perm.canEditRow(t, r)) {

            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }

        // 数据权限处理
        if (perm.isEmp()) {
            // 员工：只改完成率，自评得分自动算
            if (req.getCompletionRate() == null) {

                // 校验失败抛异常
                throw new BizException(ResultCode.BAD_REQUEST, "完成率不能为空");
            }

            r.setCompletionRate(req.getCompletionRate());
            // 调用业务服务
            r.setSelfScore(AssessmentCalcService.calcSelfScore(r.getBaseScore(), r.getCompletionRate()));
            // 更新记录
            rowMapper.updateById(r);
            // 员工填写主表信息栏：岗位
            if (req.getPosition() != null && t.getUserId().equals(DataScopeContext.currentUserId())) {

                t.setPosition(req.getPosition());
                // 调用业务服务
                tableService.updateTablePosition(t);
            }
            // 全部行按完成率重算 + 求和（保持整表一致）
            calcService.recalcByCompletionRate(t);

        // 数据权限处理
        } else if (perm.isDeptLead()) {
            // 部门领导：直接改自评得分。
            // 上限取该行指标分数的绝对值：指标分数为负数（加减分项）时表示减分，
            // 允许按「0 ~ |指标分数|」之间录入减分，负数指标分数不再阻断保存。
            if (req.getSelfScore() == null) {

                // 校验失败抛异常
                throw new BizException(ResultCode.BAD_REQUEST, "自评得分不能为空");
            }

            BigDecimal base = r.getBaseScore() == null ? BigDecimal.valueOf(100) : r.getBaseScore();
            BigDecimal max = base.abs();
            if (req.getSelfScore().compareTo(BigDecimal.ZERO) < 0

                    || req.getSelfScore().compareTo(max) > 0) {

                // 校验失败抛异常
                throw new BizException(ResultCode.BAD_REQUEST,
                        "自评得分需在 0-" + max + " 之间");
            }

            r.setSelfScore(req.getSelfScore());
            // 更新记录
            rowMapper.updateById(r);
            // 计算得分
            calcService.recalc(t);
            log.info("部门负责人改自评得分: tableId={}, rowId={}, score={}", tableId, rowId, req.getSelfScore());

        } else {

            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    // 岗位信息更新（员工在考核表信息栏填写）。
    @Transactional

    // 更新记录
    public void updatePosition(Long tableId, String position) {

        // 调用业务服务
        AssessmentTable t = tableService.getRequired(tableId);

        // 取当前用户上下文
        if (!perm.isEmp() || !t.getUserId().equals(DataScopeContext.currentUserId())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }

        // 状态判断
        if (!AssessmentState.SELF_DRAFTING.name().equals(t.getState())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED);
        }

        t.setPosition(position);
        // 调用业务服务
        tableService.updateTablePosition(t);
    }

    // 查询并返回结果
    public AssessmentRow getRow(Long rowId) {

        // 查询单条
        AssessmentRow r = rowMapper.selectById(rowId);
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        return r;
    }

    // 执行业务处理
    public boolean isBonusRow(AssessmentRow r) {

        return RowCategory.BONUS.name().equals(r.getCategory());
    }
}
