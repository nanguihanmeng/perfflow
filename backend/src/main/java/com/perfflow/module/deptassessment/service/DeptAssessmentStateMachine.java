package com.perfflow.module.deptassessment.service;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.deptassessment.enums.DeptAssessmentState;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
/**
 * 部门考核状态机：集中定义各状态的合法转移表，并在流转/校验前统一校验当前状态。
 * 已完成的考核为终态，不允许再次转移。
 */
@Component
public class DeptAssessmentStateMachine {

    private static final Map<DeptAssessmentState, Set<DeptAssessmentState>> ALLOWED =
            new EnumMap<>(DeptAssessmentState.class);

    static {
        // 未开始：启动后进入自评填报。
        ALLOWED.put(DeptAssessmentState.NOT_STARTED, EnumSet.of(DeptAssessmentState.SELF_FILLING));
        // 自评填报：提交后进入待审核（部门）。
        ALLOWED.put(DeptAssessmentState.SELF_FILLING, EnumSet.of(DeptAssessmentState.PENDING_REVIEW));
        // 待审核（部门）：审核通过进入待审批（公司），被退回则回到自评填报。
        ALLOWED.put(DeptAssessmentState.PENDING_REVIEW, EnumSet.of(
                DeptAssessmentState.PENDING_AUDIT, DeptAssessmentState.SELF_FILLING));
        // 待审批（公司）：审批通过进入已完成，被退回则回到自评填报。
        ALLOWED.put(DeptAssessmentState.PENDING_AUDIT, EnumSet.of(
                DeptAssessmentState.PENDING_APPROVE, DeptAssessmentState.SELF_FILLING));
        // 待审批（公司）：审批通过进入已完成，被退回则回到自评填报。
        ALLOWED.put(DeptAssessmentState.PENDING_APPROVE, EnumSet.of(
                DeptAssessmentState.COMPLETED, DeptAssessmentState.SELF_FILLING));
        // 已完成：无任何可转移状态。
        ALLOWED.put(DeptAssessmentState.COMPLETED, EnumSet.noneOf(DeptAssessmentState.class));
    }

    /**
     * 校验从当前状态到目标状态的转移是否合法，非法则抛业务异常。
     * @param assessment 待流转的部门考核
     * @param target 期望流转到的目标状态
     */
    public void assertTransition(DeptAssessment assessment, DeptAssessmentState target) {

        DeptAssessmentState cur = DeptAssessmentState.of(assessment.getStatus());
        // 构建集合容器
        Set<DeptAssessmentState> allow = ALLOWED.get(cur);
        // 集合包含判断
        if (!allow.contains(target)) {

            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED,
                    "当前状态 " + cur + " 不允许转移到 " + target);
        }
    }

    /**
     * 校验考核当前状态是否与期望状态一致，不一致则抛业务异常。
     * @param assessment 待校验的部门考核
     * @param expected 期望的当前状态
     */
    public void assertInState(DeptAssessment assessment, DeptAssessmentState expected) {

        DeptAssessmentState cur = DeptAssessmentState.of(assessment.getStatus());

        // 值比较
        if (cur != expected) {

            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED,
                    "当前状态 " + cur + "，需要 " + expected);
        }
    }

    /**
     * 校验通过后将部门考核状态置为目标状态（仅改内存状态，持久化由调用方负责）。
     * @param assessment 待流转的部门考核
     * @param to 目标状态
     */
    public void transition(DeptAssessment assessment, DeptAssessmentState to) {

        assertTransition(assessment, to);
        // 设置状态
        assessment.setStatus(to.getCode());
    }
}
