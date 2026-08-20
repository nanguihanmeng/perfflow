package com.perfflow.module.assessment;

import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.service.AssessmentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态机所有合法/非法转移覆盖。
 */
class AssessmentStateMachineTest {

    private AssessmentStateMachine sm;
    private AssessmentTable t;

    @BeforeEach
    void setUp() {
        sm = new AssessmentStateMachine();
        t = new AssessmentTable();
    }

    @Test
    void draft_can_submit() {
        t.setState(AssessmentState.SELF_DRAFTING.name());
        sm.transition(t, AssessmentState.SELF_SUSPENDED);
        assertEquals(AssessmentState.SELF_SUSPENDED.name(), t.getState());
    }

    @Test
    void suspended_can_push() {
        t.setState(AssessmentState.SELF_SUSPENDED.name());
        sm.transition(t, AssessmentState.DEPT_REVIEW);
        assertEquals(AssessmentState.DEPT_REVIEW.name(), t.getState());
    }

    @Test
    void review_can_approve() {
        t.setState(AssessmentState.DEPT_REVIEW.name());
        sm.transition(t, AssessmentState.LEAD_SCORING);
        assertEquals(AssessmentState.LEAD_SCORING.name(), t.getState());
    }

    @Test
    void review_can_reject_back_to_draft() {
        t.setState(AssessmentState.DEPT_REVIEW.name());
        sm.transition(t, AssessmentState.SELF_DRAFTING);
        assertEquals(AssessmentState.SELF_DRAFTING.name(), t.getState());
    }

    @Test
    void lead_can_submit_to_finished() {
        t.setState(AssessmentState.LEAD_SCORING.name());
        sm.transition(t, AssessmentState.FINISHED);
        assertEquals(AssessmentState.FINISHED.name(), t.getState());
    }

    @Test
    void finished_is_terminal() {
        t.setState(AssessmentState.FINISHED.name());
        BizException ex = assertThrows(BizException.class, () ->
                sm.transition(t, AssessmentState.DEPT_REVIEW));
        assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void draft_cannot_push_directly() {
        t.setState(AssessmentState.SELF_DRAFTING.name());
        BizException ex = assertThrows(BizException.class, () ->
                sm.transition(t, AssessmentState.DEPT_REVIEW));
        assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void suspended_cannot_approve_directly() {
        t.setState(AssessmentState.SELF_SUSPENDED.name());
        BizException ex = assertThrows(BizException.class, () ->
                sm.transition(t, AssessmentState.LEAD_SCORING));
        assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void assertInState_throws_when_mismatch() {
        t.setState(AssessmentState.DEPT_REVIEW.name());
        BizException ex = assertThrows(BizException.class, () ->
                sm.assertInState(t, AssessmentState.SELF_DRAFTING));
        assertEquals(ResultCode.STATE_NOT_ALLOWED.getCode(), ex.getCode());
    }
}
