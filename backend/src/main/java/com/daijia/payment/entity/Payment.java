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
 * 支付单
 */
@Data
@TableName("payment")
public class Payment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    /** 支付金额（分） */
    private Integer amount;

    /** MOCK / WECHAT / ALIPAY */
    private String channel;

    /** 见 PaymentStatus */
    private String status;

    /** 幂等键，数据库唯一索引兜底 */
    private String idempotencyKey;

    /** 渠道交易流水号 */
    private String tradeNo;

    private LocalDateTime payTime;

    private LocalDateTime notifyTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
