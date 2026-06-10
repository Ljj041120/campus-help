package com.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布者ID */
    private Long publisherId;

    /** 跑腿员ID */
    private Long runnerId;

    /** 跑腿类型：1-代取快递 2-代买物品 3-代送物品 4-其他 */
    private Integer orderType;

    /** 取货地址 */
    private String pickupAddress;

    /** 取货经度 */
    private BigDecimal pickupLng;

    /** 取货纬度 */
    private BigDecimal pickupLat;

    /** 送货地址 */
    private String deliveryAddress;

    /** 送货经度 */
    private BigDecimal deliveryLng;

    /** 送货纬度 */
    private BigDecimal deliveryLat;

    /** 报酬金额 */
    private BigDecimal amount;

    /** 平台服务费 */
    private BigDecimal platformFee;

    /** 跑腿员实际收入 */
    private BigDecimal runnerIncome;

    /** 订单状态 */
    private Integer status;

    /** 期望完成时间 */
    private LocalDateTime expectedTime;

    /** 物品描述 */
    private String description;

    /** 备注 */
    private String remark;

    /** 图片（逗号分隔） */
    private String images;

    /** 支付宝交易号 */
    private String alipayTradeNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
