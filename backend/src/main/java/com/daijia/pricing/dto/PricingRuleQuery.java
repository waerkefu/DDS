package com.daijia.pricing.dto;

import com.daijia.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 计价规则分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PricingRuleQuery extends PageQuery {

    private String cityCode;

    private String vehicleType;
}
