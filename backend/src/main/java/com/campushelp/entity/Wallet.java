package com.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包实体
 */
@Data
@TableName("wallets")
public class Wallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 可用余额 */
    private BigDecimal balance;

    /** 冻结金额（进行中订单的佣金） */
    private BigDecimal frozenAmount;

    /** 累计收入 */
    private BigDecimal totalIncome;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
