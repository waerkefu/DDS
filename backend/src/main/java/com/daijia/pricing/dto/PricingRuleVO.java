package com.daijia.pricing.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 计价规则视图对象。
 *
 * <p>金额字段以「分」存储，额外提供 xxxYuan 字符串（元，保留两位小数）便于展示。</p>
 */
@Data
public class PricingRuleVO implements Serializable {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Long id;
    private String ruleName;
    private String cityCode;
    private String cityName;
    private String vehicleType;
    private Integer basePrice;
    private Integer baseDistanceM;
    private Integer baseDurationS;
    private Integer pricePerKm;
    private Integer pricePerMin;
    private Integer minPrice;
    private LocalTime nightStart;
    private LocalTime nightEnd;
    private BigDecimal nightRatio;
    private BigDecimal bookingRatio;
    private Integer status;
    private LocalDateTime createTime;

    public String getBasePriceYuan() {
        return toYuan(basePrice);
    }

    public String getPricePerKmYuan() {
        return toYuan(pricePerKm);
    }

    public String getPricePerMinYuan() {
        return toYuan(pricePerMin);
    }

    public String getMinPriceYuan() {
        return toYuan(minPrice);
    }

    private static String toYuan(Integer fen) {
        if (fen == null) {
            return null;
        }
        return BigDecimal.valueOf(fen).divide(HUNDRED, 2, RoundingMode.HALF_UP).toPlainString();
    }
}
