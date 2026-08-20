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
        private String type;       // SELF_DRAFTING / SUSPEND_SOON / LEAD_SCORING ...
        private String title;
        private String description;
        private Long   targetTableId;
        private Integer severity;  // 1=info, 2=warn, 3=urgent
    }
}
