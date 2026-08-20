package com.perfflow.module.period.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 开启周期请求体。
 *
 * <p>userIds 为参与考核的员工 ID 列表；为空时兼容旧逻辑（为全员生成考核表）。
 */
@Data
public class PeriodOpenReq implements Serializable {

    /** 参与考核的员工 ID 列表，为空表示全员 */
    private List<Long> userIds;
}
