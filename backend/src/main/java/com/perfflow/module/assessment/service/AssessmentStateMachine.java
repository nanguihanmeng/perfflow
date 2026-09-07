package com.perfflow.module.assessment.service;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.entity.AssessmentTable;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
/**
 * 个人考核状态机：集中维护各状态间合法转移表，供流程推进与校验统一使用；
 * 已完成(FINISHED)为终态，不允许再转移。
 */
@Component
public class AssessmentStateMachine {

    private static final Map<AssessmentState, Set<AssessmentState>> ALLOWED = new EnumMap<>(AssessmentState.class);

    static {
        // 填报中：可申请挂起进入挂起中。
        ALLOWED.put(AssessmentState.SELF_DRAFTING, EnumSet.of(AssessmentState.SELF_SUSPENDED));
        // 挂起中：挂起到期后进入待部门审核。
        ALLOWED.put(AssessmentState.SELF_SUSPENDED, EnumSet.of(AssessmentState.DEPT_REVIEW));
        // 待部门审核：审核通过进入待领导评分，被退回回到填报中。
        ALLOWED.put(AssessmentState.DEPT_REVIEW, EnumSet.of(
                AssessmentState.LEAD_SCORING, AssessmentState.SELF_DRAFTING));
        // 待领导评分：评分完成后进入已完成。
        ALLOWED.put(AssessmentState.LEAD_SCORING, EnumSet.of(AssessmentState.FINISHED));
        // 已完成：终态，不允许再转移。
        ALLOWED.put(AssessmentState.FINISHED, EnumSet.noneOf(AssessmentState.class));
    }

    /**
     * 校验主表当前状态能否转移到目标状态，非法则抛业务异常。
     * @param table 个人考核主表
     * @param target 期望流转到的目标状态
     */
    public void assertTransition(AssessmentTable table, AssessmentState target) {

        AssessmentState cur = AssessmentState.valueOf(table.getState());
        // 构建集合容器
        Set<AssessmentState> allow = ALLOWED.get(cur);
        // 集合包含判断
        if (!allow.contains(target)) {

            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED,
                    "当前状态 " + cur.name() + " 不允许转移到 " + target.name());
        }
    }

    /**
     * 校验主表当前状态是否等于期望状态，不一致则抛业务异常。
     * @param table 个人考核主表
     * @param expected 期望的当前状态
     */
    public void assertInState(AssessmentTable table, AssessmentState expected) {

        AssessmentState cur = AssessmentState.valueOf(table.getState());

        // 值比较
        if (cur != expected) {

            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED,
                    "当前状态 " + cur.name() + "，需要 " + expected.name());
        }
    }

    /**
     * 校验通过后将主表状态置为目标状态（仅改内存状态，持久化由调用方负责）。
     * @param table 个人考核主表
     * @param to 目标状态
     */
    public void transition(AssessmentTable table, AssessmentState to) {

        assertTransition(table, to);
        table.setState(to.name());
    }
}
