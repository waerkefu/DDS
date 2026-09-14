package com.daijia.order.controller;

import com.daijia.common.PageResult;
import com.daijia.common.Result;
import com.daijia.order.dto.CancelOrderRequest;
import com.daijia.order.dto.OrderCreateRequest;
import com.daijia.order.dto.OrderDetailVO;
import com.daijia.order.dto.OrderQuery;
import com.daijia.order.dto.OrderVO;
import com.daijia.order.service.OrderService;
import com.daijia.pricing.dto.PriceEstimateRequest;
import com.daijia.pricing.dto.PriceEstimateVO;
import com.daijia.pricing.service.PricingService;
import com.daijia.security.RequireRole;
import com.daijia.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户订单接口
 */
@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@RequireRole("CUSTOMER")
public class CustomerOrderController {

    private final OrderService orderService;
    private final PricingService pricingService;

    /** 价格预估 */
    @PostMapping("/estimate")
    public Result<PriceEstimateVO> estimate(@Valid @RequestBody PriceEstimateRequest req) {
        return Result.ok(pricingService.estimate(req));
    }

    /** 下单，支持 X-Idempotency-Key 幂等 */
    @PostMapping
    public Result<OrderVO> create(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderCreateRequest req) {
        return Result.ok(orderService.create(UserContext.currentUserId(), req, idempotencyKey));
    }

    @GetMapping
    public Result<PageResult<OrderVO>> page(OrderQuery query) {
        return Result.ok(orderService.pageForCustomer(UserContext.currentUserId(), query));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.customerDetail(UserContext.currentUserId(), orderNo));
    }

    @PutMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo,
                               @RequestBody(required = false) CancelOrderRequest req) {
        orderService.cancelByCustomer(UserContext.currentUserId(), orderNo,
                req == null ? null : req.getReason());
        return Result.ok();
    }
}
