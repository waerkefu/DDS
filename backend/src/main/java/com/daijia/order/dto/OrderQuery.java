package com.daijia.order.dto;

import com.daijia.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 订单分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQuery extends PageQuery {

    private String status;

    private String orderType;

    private String orderNo;

    private Long customerId;

    private Long providerId;

    /** 创建时间起（含） */
    private LocalDateTime startTime;

    /** 创建时间止（含） */
    private LocalDateTime endTime;
}
