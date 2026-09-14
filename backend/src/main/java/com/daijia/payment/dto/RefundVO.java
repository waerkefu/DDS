package com.daijia.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款单视图对象
 */
@Data
public class RefundVO implements Serializable {

    private Long id;

    private String refundNo;

    private String orderNo;

    private String payNo;

    /** 退款金额（分） */
    private Integer amount;

    /** 退款金额（元） */
    private String amountYuan;

    private String status;

    private String reason;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;
}
