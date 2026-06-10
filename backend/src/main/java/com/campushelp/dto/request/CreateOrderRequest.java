package com.campushelp.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateOrderRequest {
    @NotNull(message = "跑腿类型不能为空")
    private Integer orderType;

    @NotBlank(message = "取货地址不能为空")
    private String pickupAddress;

    @NotNull(message = "取货经度不能为空")
    private BigDecimal pickupLng;

    @NotNull(message = "取货纬度不能为空")
    private BigDecimal pickupLat;

    @NotBlank(message = "送货地址不能为空")
    private String deliveryAddress;

    @NotNull(message = "送货经度不能为空")
    private BigDecimal deliveryLng;

    @NotNull(message = "送货纬度不能为空")
    private BigDecimal deliveryLat;

    @DecimalMin(value = "2.0", message = "报酬金额不能低于2元")
    @DecimalMax(value = "50.0", message = "报酬金额不能超过50元")
    @NotNull(message = "报酬金额不能为空")
    private BigDecimal amount;

    private String description;
    private String remark;
    private String images;

    @NotNull(message = "期望完成时间不能为空")
    private LocalDateTime expectedTime;
}
