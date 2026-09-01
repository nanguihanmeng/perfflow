package com.perfflow.module.deptassessment.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
// 部门 KPI 行实际完成值。
@Data
public class DeptActualValueRowReq implements Serializable {

    @NotNull
    // 序号。
    private Integer seqNo;
    private String actualValue;
}
