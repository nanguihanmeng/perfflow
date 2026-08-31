package com.perfflow.module.deptassessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门 KPI 行实际完成值。
 */
@Data
public class DeptActualValueRowReq implements Serializable {

    /** 模板行序号（1-7） */
    @NotNull
    private Integer seqNo;

    /** 实际完成值 */
    private String actualValue;
}
