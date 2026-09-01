package com.perfflow.module.deptassessment.service;

import com.perfflow.module.deptassessment.dto.DeptFlowLogResp;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.entity.DeptAssessmentFlowLog;
import com.perfflow.module.deptassessment.enums.DeptAssessmentState;
import com.perfflow.module.deptassessment.mapper.DeptAssessmentFlowLogMapper;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部门考核流程日志服务覆盖。
 */
class DeptAssessmentFlowServiceTest {

    private DeptAssessmentFlowLogMapper logMapper;
    private SysUserMapper userMapper;
    private DeptAssessmentFlowService service;

    @BeforeEach
    void setUp() {
        logMapper = mock(DeptAssessmentFlowLogMapper.class);
        userMapper = mock(SysUserMapper.class);
        service = new DeptAssessmentFlowService(logMapper, userMapper);
        // 模拟当前登录用户
        DataScopeContext.set(DataScopeContext.CurrentUser.builder()
                .userId(1L).username("deptstaff").primaryRole("DEPT_STAFF").build());
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
    }

    @Test
    void writeLog_records_operator_and_transition() {
        DeptAssessment assessment = new DeptAssessment();
        assessment.setId(10L);
        service.writeLog(assessment, DeptAssessmentState.SELF_FILLING,
                DeptAssessmentState.PENDING_REVIEW,
                DeptAssessmentFlowService.ACTION_SUBMIT, "完成填报");

        // 校验插入的实体字段
        verify(logMapper).insert(any(DeptAssessmentFlowLog.class));
    }

    @Test
    void listLogs_maps_operator_name() {
        DeptAssessmentFlowLog log = new DeptAssessmentFlowLog();
        log.setId(1L);
        log.setAssessmentId(10L);
        log.setFromStatus(DeptAssessmentState.SELF_FILLING.getCode());
        log.setToStatus(DeptAssessmentState.PENDING_REVIEW.getCode());
        log.setAction(DeptAssessmentFlowService.ACTION_SUBMIT);
        log.setOperatorId(1L);
        log.setOperatorRole("DEPT_STAFF");
        when(logMapper.selectList(any())).thenReturn(List.of(log));
        SysUser user = new SysUser();
        user.setId(1L);
        user.setRealName("刘专员");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user));

        List<DeptFlowLogResp> result = service.listLogs(10L);
        assertEquals(1, result.size());
        DeptFlowLogResp resp = result.get(0);
        assertNotNull(resp.getOperatorName());
        assertEquals("刘专员", resp.getOperatorName());
        assertEquals(DeptAssessmentState.SELF_FILLING.getCode(), resp.getFromStatus());
        assertEquals(DeptAssessmentState.PENDING_REVIEW.getCode(), resp.getToStatus());
    }

    @Test
    void listLogs_returns_empty_when_no_logs() {
        when(logMapper.selectList(any())).thenReturn(List.of());
        List<DeptFlowLogResp> result = service.listLogs(99L);
        assertEquals(0, result.size());
    }
}
