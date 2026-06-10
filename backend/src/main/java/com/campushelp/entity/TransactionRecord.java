package com.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水实体
 */
@Data
@TableName("transactions")
public class TransactionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 钱包ID */
    private Long walletId;

    /** 订单ID */
    private Long orderId;

    /** 交易类型：1-订单支付 2-订单结算 3-提现 4-退款 5-平台佣金 */
    private Integer type;

    /** 交易金额（正数为收入，负数为支出） */
    private BigDecimal amount;

    /** 交易后余额 */
    private BigDecimal balanceAfter;

    /** 描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
