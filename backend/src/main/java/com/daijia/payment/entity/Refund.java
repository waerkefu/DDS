package com.daijia.payment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款单
 */
@Data
@TableName("refund")
public class Refund implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long paymentId;

    private String payNo;

    private Long orderId;

    private String orderNo;

    /** 退款金额（分） */
    private Integer amount;

    private String reason;

    /** 见 RefundStatus */
    private String status;

    private String idempotencyKey;

    private LocalDateTime refundTime;

    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
