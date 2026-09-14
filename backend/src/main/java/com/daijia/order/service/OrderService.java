package com.daijia.order.service;

import com.daijia.common.PageQuery;
import com.daijia.common.PageResult;
import com.daijia.order.dto.FinishServiceRequest;
import com.daijia.order.dto.OrderCreateRequest;
import com.daijia.order.dto.OrderDetailVO;
import com.daijia.order.dto.OrderQuery;
import com.daijia.order.dto.OrderVO;
import com.daijia.order.entity.Orders;

/**
 * 订单服务。
 *
 * <p>所有状态变更均在事务内按「校验 → 条件更新 → 写日志」的顺序完成。</p>
 */
public interface OrderService {

    OrderVO create(Long customerId, OrderCreateRequest req, String idempotencyKey);

    PageResult<OrderVO> pageForCustomer(Long customerId, OrderQuery query);

    OrderDetailVO customerDetail(Long customerId, String orderNo);

    void cancelByCustomer(Long customerId, String orderNo, String reason);

    PageResult<OrderVO> pageHall(Long providerUserId, PageQuery query);

    OrderVO grab(Long providerUserId, String orderNo);

    OrderVO startService(Long providerUserId, String orderNo);

    OrderVO finishService(Long providerUserId, String orderNo, FinishServiceRequest req);

    PageResult<OrderVO> pageForProvider(Long providerUserId, OrderQuery query);

    OrderDetailVO providerDetail(Long providerUserId, String orderNo);

    PageResult<OrderVO> pageForAdmin(OrderQuery query);

    OrderDetailVO adminDetail(String orderNo);

    void cancelByAdmin(String orderNo, String reason);

    Orders getByOrderNoOrThrow(String orderNo);

    int closeExpiredOrders();
}
