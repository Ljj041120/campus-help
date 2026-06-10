package com.campushelp.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    WAITING_FOR_ACCEPT(1, "待接单"),
    ACCEPTED(2, "已接单"),
    IN_PROGRESS(3, "进行中"),
    PENDING_CONFIRM(4, "待确认"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消"),
    APPEALING(7, "申诉中");

    private final int value;
    private final String label;

    OrderStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 根据值获取枚举
     */
    public static OrderStatus fromValue(int value) {
        for (OrderStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown OrderStatus value: " + value);
    }
}
