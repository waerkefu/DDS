package com.daijia.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务单号生成器：前缀 + 时间戳 + 随机数，长度不超过 32。
 */
public final class NoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private NoGenerator() {
    }

    /** 订单号 */
    public static String orderNo() {
        return generate("DO");
    }

    /** 支付单号 */
    public static String payNo() {
        return generate("PY");
    }

    /** 退款单号 */
    public static String refundNo() {
        return generate("RF");
    }

    /** 服务商编号 */
    public static String providerNo() {
        return generate("SP");
    }

    private static String generate(String prefix) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return prefix + timestamp + random;
    }
}
