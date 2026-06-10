package com.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实名认证实体
 */
@Data
@TableName("real_name_auth")
public class RealNameAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 学号/工号 */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 学生证照片URL */
    private String idCardPhoto;

    /** 审核状态：0-待审核 1-已通过 2-已拒绝 */
    private Integer status;

    /** 审核意见 */
    private String auditComment;

    /** 审核人ID */
    private Long auditorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
