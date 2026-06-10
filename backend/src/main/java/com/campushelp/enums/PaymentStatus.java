package com.campushelp.enums;

import lombok.Getter;

/**
 * 支付状态枚举
 */
@Getter
public enum PaymentStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款"),
    FAILED(3, "支付失败");

    private final int value;
    private final String label;

    PaymentStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }
}
