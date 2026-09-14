package com.daijia.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付单视图对象
 */
@Data
public class PaymentVO implements Serializable {

    private Long id;

    private String payNo;

    private String orderNo;

    /** 支付金额（分） */
    private Integer amount;

    /** 支付金额（元） */
    private String amountYuan;

    private String channel;

    private String status;

    private LocalDateTime payTime;

    private LocalDateTime createTime;
}
