package com.perfflow.module.period.dto;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
 // 对应列表为空表示全员/全部部门。
@Data
public class PeriodOpenReq implements Serializable {

    private List<Long> userIds;
    private List<Long> deptIds;
}
