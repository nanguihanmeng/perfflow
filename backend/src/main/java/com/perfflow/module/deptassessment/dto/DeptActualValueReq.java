package com.perfflow.module.deptassessment.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
// 部门考核填报请求（绩效专员仅提交各 KPI 行的实际完成值）。
@Data
public class DeptActualValueReq implements Serializable {

    @NotNull
    // 明细行列表。
    private List<DeptActualValueRowReq> rows;
}
