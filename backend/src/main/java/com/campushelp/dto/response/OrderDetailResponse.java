package com.campushelp.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailResponse {
    private Long id;
    private Long publisherId;
    private String publisherNickname;
    private Long runnerId;
    private String runnerNickname;
    private Integer orderType;
    private String pickupAddress;
    private BigDecimal pickupLng;
    private BigDecimal pickupLat;
    private String deliveryAddress;
    private BigDecimal deliveryLng;
    private BigDecimal deliveryLat;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal runnerIncome;
    private Integer status;
    private LocalDateTime expectedTime;
    private String description;
    private String remark;
    private String images;
    private LocalDateTime createdAt;
}
