package com.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体
 */
@Data
@TableName("evaluations")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 评价者ID */
    private Long fromUserId;

    /** 被评价者ID */
    private Long toUserId;

    /** 评分（1-5） */
    private Integer score;

    /** 评价内容 */
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
