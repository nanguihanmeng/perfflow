package com.perfflow.module.period.dto;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.enums.PeriodType;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
@Data
public class PeriodResp implements Serializable {
    // ID。
    private Long id;
    // 名称。
    private String name;
    private String periodType;
    private String periodTypeLabel;
    // 年份。
    private Integer year;
    // 季度。
    private Integer quarter;
    // 开始日期。
    private LocalDate startDate;
    // 自评截止日期。
    private LocalDate suspendEndDate;
    // 部门审核截止日期。
    private LocalDate deptReviewEndDate;
    // 领导评分截止日期。
    private LocalDate leadScoreEndDate;
    // 到期是否自动推送。
    private Boolean autoPushOnExpire;
    private Integer status;
    // 执行 from。

    public static PeriodResp from(AssessmentPeriod p) {

        PeriodResp r = new PeriodResp();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setPeriodType(p.getPeriodType());
        PeriodType type = p.getPeriodType() == null ? null : PeriodType.of(p.getPeriodType());
        r.setPeriodTypeLabel(type == null ? null : type.getLabel());
        r.setYear(p.getYear());
        r.setQuarter(p.getQuarter());
        r.setStartDate(p.getStartDate());
        r.setSuspendEndDate(p.getSuspendEndDate());
        r.setDeptReviewEndDate(p.getDeptReviewEndDate());
        r.setLeadScoreEndDate(p.getLeadScoreEndDate());
        r.setAutoPushOnExpire(p.getAutoPushOnExpire());
        r.setStatus(p.getStatus());
        return r;
    }
}
