package com.campushelp.enums;

import lombok.Getter;

/**
 * 实名认证状态枚举
 */
@Getter
public enum AuthStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final int value;
    private final String label;

    AuthStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }
}
