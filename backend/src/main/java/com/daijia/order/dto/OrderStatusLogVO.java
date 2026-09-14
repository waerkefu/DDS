package com.daijia.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态流转日志视图对象
 */
@Data
public class OrderStatusLogVO implements Serializable {

    private String fromStatus;
    private String toStatus;
    private String fromStatusDesc;
    private String toStatusDesc;
    private Integer operatorType;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
