package com.daijia.order.service;

import com.daijia.common.BusinessException;
import com.daijia.common.ResultCode;
import com.daijia.order.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机。
 *
 * <p>所有订单状态流转必须经过本类校验，未在流转表中声明的变更一律拒绝，
 * 从源头避免出现非法状态与脏数据。</p>
 *
 * <pre>
 * PENDING_ACCEPT ──► ACCEPTED ──► IN_SERVICE ──► PENDING_PAYMENT ──► COMPLETED
 *       │                │                              │
 *       ├──► CANCELLED   ├──► CANCELLED                 └──► CANCELLED
 *       └──► CLOSED
 * </pre>
 */
@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS;

    static {
        Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
        transitions.put(OrderStatus.PENDING_ACCEPT,
                EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED, OrderStatus.CLOSED));
        transitions.put(OrderStatus.ACCEPTED,
                EnumSet.of(OrderStatus.IN_SERVICE, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.IN_SERVICE,
                EnumSet.of(OrderStatus.PENDING_PAYMENT));
        transitions.put(OrderStatus.PENDING_PAYMENT,
                EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        // 终态：不可再流转
        transitions.put(OrderStatus.COMPLETED, Collections.emptySet());
        transitions.put(OrderStatus.CANCELLED, Collections.emptySet());
        transitions.put(OrderStatus.CLOSED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    /**
     * 判断状态流转是否合法
     */
    public boolean canTransit(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    /**
     * 校验状态流转，非法时抛出业务异常
     */
    public void assertCanTransit(OrderStatus from, OrderStatus to) {
        if (!canTransit(from, to)) {
            throw new BusinessException(ResultCode.ORDER_STATUS_NOT_ALLOWED,
                    String.format("订单当前状态为「%s」，不允许变更为「%s」",
                            from == null ? "未知" : from.getDescription(),
                            to == null ? "未知" : to.getDescription()));
        }
    }

    /**
     * 查询某状态下允许流转到的后续状态
     */
    public Set<OrderStatus> nextStatuses(OrderStatus from) {
        return TRANSITIONS.getOrDefault(from, Collections.emptySet());
    }
}
