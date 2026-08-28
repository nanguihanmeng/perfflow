package com.perfflow.module.period.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 开启周期请求体。
 *
 * <p>个人线类型传 {@link #userIds}（参与被考核人），部门线类型传 {@link #deptIds}（参与部门）；
 * 对应列表为空表示全员/全部部门。
 */
@Data
public class PeriodOpenReq implements Serializable {

    /** 参与考核的员工 ID 列表（个人线），为空表示全员 */
    private List<Long> userIds;

    /** 参与考核的部门 ID 列表（部门线），为空表示全部部门 */
    private List<Long> deptIds;
}
