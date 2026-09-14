package com.daijia.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 订单视图对象。
 *
 * <p>金额字段以「分」存储，额外提供 xxxYuan 字符串（元，保留两位小数）便于展示。</p>
 */
@Data
public class OrderVO implements Serializable {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Long id;
    private String orderNo;
    private String orderType;
    private String orderTypeDesc;
    private String status;
    private String statusDesc;
    private Long customerId;
    private String customerName;
    private Long providerId;
    private String providerName;
    private String cityCode;
    private String vehicleType;
    private String vehiclePlate;
    private String contactName;
    private String contactPhone;
    private String startAddress;
    private String endAddress;
    private LocalDateTime bookingTime;
    private LocalDateTime acceptTime;
    private LocalDateTime serviceStartTime;
    private LocalDateTime serviceEndTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private Integer distanceM;
    private Integer durationS;
    private Integer estimateAmount;
    private Integer actualAmount;
    private Integer payAmount;
    private Integer refundAmount;
    private String payStatus;
    private String payStatusDesc;
    private String remark;
    private LocalDateTime createTime;

    public String getEstimateAmountYuan() {
        return toYuan(estimateAmount);
    }

    public String getActualAmountYuan() {
        return toYuan(actualAmount);
    }

    public String getPayAmountYuan() {
        return toYuan(payAmount);
    }

    public String getRefundAmountYuan() {
        return toYuan(refundAmount);
    }

    private static String toYuan(Integer fen) {
        if (fen == null) {
            return null;
        }
        return BigDecimal.valueOf(fen).divide(HUNDRED, 2, RoundingMode.HALF_UP).toPlainString();
    }
}
