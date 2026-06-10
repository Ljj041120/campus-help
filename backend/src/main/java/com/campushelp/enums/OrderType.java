package com.campushelp.enums;

import lombok.Getter;

/**
 * 跑腿类型枚举
 */
@Getter
public enum OrderType {
    PICKUP(1, "代取快递"),
    BUY(2, "代买物品"),
    DELIVERY(3, "代送物品"),
    OTHER(4, "其他");

    private final int value;
    private final String label;

    OrderType(int value, String label) {
        this.value = value;
        this.label = label;
    }
}
