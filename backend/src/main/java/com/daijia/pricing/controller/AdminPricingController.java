package com.daijia.pricing.controller;

import com.daijia.common.PageResult;
import com.daijia.common.Result;
import com.daijia.pricing.dto.PricingRuleQuery;
import com.daijia.pricing.dto.PricingRuleSaveRequest;
import com.daijia.pricing.dto.PricingRuleVO;
import com.daijia.pricing.service.PricingService;
import com.daijia.security.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端计价规则维护
 */
@RestController
@RequestMapping("/api/admin/pricing-rules")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminPricingController {

    private final PricingService pricingService;

    @GetMapping
    public Result<PageResult<PricingRuleVO>> page(PricingRuleQuery query) {
        return Result.ok(pricingService.page(query));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PricingRuleSaveRequest req) {
        return Result.ok(pricingService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PricingRuleSaveRequest req) {
        pricingService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        pricingService.delete(id);
        return Result.ok();
    }
}
