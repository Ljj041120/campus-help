package com.campushelp.enums;

import lombok.Getter;

/**
 * 交易类型枚举
 */
@Getter
public enum TransactionType {
    ORDER_PAY(1, "订单支付"),
    ORDER_SETTLE(2, "订单结算"),
    WITHDRAW(3, "提现"),
    REFUND(4, "退款"),
    COMMISSION(5, "平台佣金");

    private final int value;
    private final String label;

    TransactionType(int value, String label) {
        this.value = value;
        this.label = label;
    }
}
