package com.perfflow.common.enums;
import lombok.Getter;
// 通知类型。
@Getter
public enum NotificationType {

    SYSTEM("SYSTEM"),
    REMIND("REMIND"),
    OVERDUE("OVERDUE"),
    REJECT("REJECT");
    // 编码
    private final String code;

    NotificationType(String code) {
        this.code = code;
    }
}
