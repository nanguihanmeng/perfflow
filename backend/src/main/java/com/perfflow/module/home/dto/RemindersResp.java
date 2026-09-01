package com.perfflow.module.home.dto;
import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@Data
public class RemindersResp implements Serializable {

    private List<Reminder> todos = new ArrayList<>();
    private List<Reminder> upcomingSuspends = new ArrayList<>();
    private List<Reminder> systemNotices = new ArrayList<>();
    @Data
    public static class Reminder implements Serializable {

        private String bizType;
        private String type;
        // 标题。
        private String title;
        // 描述。
        private String description;
        private Long targetTableId;
        private Long targetAssessmentId;
        private Long targetPeriodId;
        private Integer severity;
    }
}
