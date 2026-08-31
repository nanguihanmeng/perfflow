package com.perfflow.module.home.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class RemindersResp implements Serializable {
    /** 当前用户待办摘要 */
    private List<Reminder> todos = new ArrayList<>();
    /** 挂起结束前 3 天提醒 */
    private List<Reminder> upcomingSuspends = new ArrayList<>();
    /** 公示提醒（全公司已完成情况） */
    private List<Reminder> systemNotices = new ArrayList<>();

    @Data
    public static class Reminder implements Serializable {
        /** 业务类型：PERSONAL=个人考核 / DEPT=部门考核 / SUSPEND_SOON=挂起预警 */
        private String bizType;
        /** 状态/类型编码：SELF_DRAFTING / DEPT_1 / SUSPEND_SOON ... */
        private String type;
        private String title;
        private String description;
        /** 个人考核主表ID（跳转个人考核详情用） */
        private Long targetTableId;
        /** 部门考核主表ID（跳转部门考核详情用） */
        private Long targetAssessmentId;
        /** 周期ID */
        private Long targetPeriodId;
        /** 1=info, 2=warn, 3=urgent */
        private Integer severity;
    }
}
