package com.perfflow.module.deptassessment.service;

import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.enums.DeptAssessmentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 部门考核状态机合法/非法转移覆盖。
 */
class DeptAssessmentStateMachineTest {

    private DeptAssessmentStateMachine sm;
    private DeptAssessment assessment;

    @BeforeEach
    void setUp() {
        sm = new DeptAssessmentStateMachine();
        assessment = new DeptAssessment();
    }

    @Test
    void filling_can_submit_to_review() {
        assessment.setStatus(DeptAssessmentState.SELF_FILLING.getCode());
        sm.transition(assessment, DeptAssessmentState.PENDING_REVIEW);
        assertEquals(DeptAssessmentState.PENDING_REVIEW.getCode(), assessment.getStatus());
    }

    @Test
    void review_can_pass_to_audit() {
        assessment.setStatus(DeptAssessmentState.PENDING_REVIEW.getCode());
        sm.transition(assessment, DeptAssessmentState.PENDING_AUDIT);
        assertEquals(DeptAssessmentState.PENDING_AUDIT.getCode(), assessment.getStatus());
    }

    @Test
    void review_can_reject_to_filling() {
        assessment.setStatus(DeptAssessmentState.PENDING_REVIEW.getCode());
        sm.transition(assessment, DeptAssessmentState.SELF_FILLING);
        assertEquals(DeptAssessmentState.SELF_FILLING.getCode(), assessment.getStatus());
    }

    @Test
    void audit_can_pass_to_approve() {
        assessment.setStatus(DeptAssessmentState.PENDING_AUDIT.getCode());
        sm.transition(assessment, DeptAssessmentState.PENDING_APPROVE);
        assertEquals(DeptAssessmentState.PENDING_APPROVE.getCode(), assessment.getStatus());
    }

    @Test
    void audit_can_reject_to_filling() {
        assessment.setStatus(DeptAssessmentState.PENDING_AUDIT.getCode());
        sm.transition(assessment, DeptAssessmentState.SELF_FILLING);
        assertEquals(DeptAssessmentState.SELF_FILLING.getCode(), assessment.getStatus());
    }

    @Test
    void approve_can_complete() {
        assessment.setStatus(DeptAssessmentState.PENDING_APPROVE.getCode());
        sm.transition(assessment, DeptAssessmentState.COMPLETED);
        assertEquals(DeptAssessmentState.COMPLETED.getCode(), assessment.getStatus());
    }

    @Test
    void complete_is_terminal() {
        assessment.setStatus(DeptAssessmentState.COMPLETED.getCode());
        BizException ex = assertThrows(BizException.class, () ->
                sm.transition(assessment, DeptAssessmentState.PENDING_REVIEW));
        assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void filling_cannot_skip_to_audit() {
        assessment.setStatus(DeptAssessmentState.SELF_FILLING.getCode());
        BizException ex = assertThrows(BizException.class, () ->
                sm.transition(assessment, DeptAssessmentState.PENDING_AUDIT));
        assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void not_started_can_open_to_filling() {
        assessment.setStatus(DeptAssessmentState.NOT_STARTED.getCode());
        sm.transition(assessment, DeptAssessmentState.SELF_FILLING);
        assertEquals(DeptAssessmentState.SELF_FILLING.getCode(), assessment.getStatus());
    }

    @Test
    void assertInState_throws_on_mismatch() {
        assessment.setStatus(DeptAssessmentState.PENDING_REVIEW.getCode());
        BizException ex = assertThrows(BizException.class, () ->
                sm.assertInState(assessment, DeptAssessmentState.SELF_FILLING));
        assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
    }
}
