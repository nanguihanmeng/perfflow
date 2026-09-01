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
// 状态机：定义合法转移并校验当前状态是否允许。
@Component
public class AssessmentStateMachine {

    private static final Map<AssessmentState, Set<AssessmentState>> ALLOWED = new EnumMap<>(AssessmentState.class);

    static {
        ALLOWED.put(AssessmentState.SELF_DRAFTING, EnumSet.of(AssessmentState.SELF_SUSPENDED));
        ALLOWED.put(AssessmentState.SELF_SUSPENDED, EnumSet.of(AssessmentState.DEPT_REVIEW));
        ALLOWED.put(AssessmentState.DEPT_REVIEW, EnumSet.of(
                AssessmentState.LEAD_SCORING, AssessmentState.SELF_DRAFTING));
        ALLOWED.put(AssessmentState.LEAD_SCORING, EnumSet.of(AssessmentState.FINISHED));
        ALLOWED.put(AssessmentState.FINISHED, EnumSet.noneOf(AssessmentState.class));
    }

    // 执行业务处理
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

    // 执行业务处理
    public void assertInState(AssessmentTable table, AssessmentState expected) {

        AssessmentState cur = AssessmentState.valueOf(table.getState());

        // 值比较
        if (cur != expected) {

            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED,
                    "当前状态 " + cur.name() + "，需要 " + expected.name());
        }
    }

    // 流转状态。

    // 执行业务处理
    public void transition(AssessmentTable table, AssessmentState to) {

        assertTransition(table, to);
        table.setState(to.name());
    }
}
