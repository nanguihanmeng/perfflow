package com.perfflow.module.period.dto;

import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.enums.PeriodType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class PeriodResp implements Serializable {
    private Long id;
    private String name;
    /** 周期类型编码，见 {@link PeriodType} */
    private String periodType;
    /** 周期类型中文名 */
    private String periodTypeLabel;
    private Integer year;
    private Integer quarter;
    private LocalDate startDate;
    private LocalDate suspendEndDate;
    private LocalDate deptReviewEndDate;
    private LocalDate leadScoreEndDate;
    private Boolean autoPushOnExpire;
    /** 0=未开始 1=进行中 2=已结束 */
    private Integer status;

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
