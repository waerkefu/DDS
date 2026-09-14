package com.daijia.payment.controller;

import com.daijia.common.Result;
import com.daijia.payment.dto.PaymentCreateVO;
import com.daijia.payment.dto.PaymentVO;
import com.daijia.payment.service.PaymentService;
import com.daijia.security.LoginUser;
import com.daijia.security.RequireRole;
import com.daijia.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口（客户 / 管理员）
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final PaymentService paymentService;

    /**
     * 发起支付，请求头 X-Idempotency-Key 可选
     */
    @PostMapping("/orders/{orderNo}/pay")
    @RequireRole({"CUSTOMER", "ADMIN"})
    public Result<PaymentCreateVO> pay(
            @PathVariable String orderNo,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        LoginUser loginUser = UserContext.require();
        PaymentCreateVO vo = loginUser.hasRole(ROLE_ADMIN)
                ? paymentService.createPaymentForAdmin(orderNo, idempotencyKey)
                : paymentService.createPayment(loginUser.getUserId(), orderNo, idempotencyKey);
        return Result.ok(vo);
    }

    /**
     * 查询订单的支付单
     */
    @GetMapping("/orders/{orderNo}")
    @RequireRole({"CUSTOMER", "ADMIN"})
    public Result<PaymentVO> getByOrderNo(@PathVariable String orderNo) {
        return Result.ok(paymentService.getByOrderNo(orderNo));
    }
}
