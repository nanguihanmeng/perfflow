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
 * 部门考核状态机：定义合法转移并校验当前状态是否允许。
 */
@Component
public class DeptAssessmentStateMachine {

    private static final Map<DeptAssessmentState, Set<DeptAssessmentState>> ALLOWED =
            new EnumMap<>(DeptAssessmentState.class);

    static {
        ALLOWED.put(DeptAssessmentState.NOT_STARTED,     EnumSet.of(DeptAssessmentState.SELF_FILLING));
        ALLOWED.put(DeptAssessmentState.SELF_FILLING,    EnumSet.of(DeptAssessmentState.PENDING_REVIEW));
        ALLOWED.put(DeptAssessmentState.PENDING_REVIEW,  EnumSet.of(DeptAssessmentState.PENDING_AUDIT, DeptAssessmentState.SELF_FILLING));
        ALLOWED.put(DeptAssessmentState.PENDING_AUDIT,   EnumSet.of(DeptAssessmentState.PENDING_APPROVE, DeptAssessmentState.SELF_FILLING));
        ALLOWED.put(DeptAssessmentState.PENDING_APPROVE, EnumSet.of(DeptAssessmentState.COMPLETED, DeptAssessmentState.SELF_FILLING));
        ALLOWED.put(DeptAssessmentState.COMPLETED,       EnumSet.noneOf(DeptAssessmentState.class));
    }

    public void assertTransition(DeptAssessment assessment, DeptAssessmentState target) {
        DeptAssessmentState cur = DeptAssessmentState.of(assessment.getStatus());
        Set<DeptAssessmentState> allow = ALLOWED.get(cur);
        if (!allow.contains(target)) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED,
                    "当前状态 " + cur + " 不允许转移到 " + target);
        }
    }

    public void assertInState(DeptAssessment assessment, DeptAssessmentState expected) {
        DeptAssessmentState cur = DeptAssessmentState.of(assessment.getStatus());
        if (cur != expected) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED,
                    "当前状态 " + cur + "，需要 " + expected);
        }
    }

    public void transition(DeptAssessment assessment, DeptAssessmentState to) {
        assertTransition(assessment, to);
        assessment.setStatus(to.getCode());
    }
}
