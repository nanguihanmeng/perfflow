package com.perfflow.common.enums;

import lombok.Getter;

/**
 * 通知类型。
 */
@Getter
public enum NotificationType {

    /** 系统提醒 */
    SYSTEM("SYSTEM"),
    /** 催办提醒 */
    REMIND("REMIND"),
    /** 超时预警 */
    OVERDUE("OVERDUE"),
    /** 退回整改 */
    REJECT("REJECT");

    private final String code;

    NotificationType(String code) {
        this.code = code;
    }
}
