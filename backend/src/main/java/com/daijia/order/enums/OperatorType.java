package com.daijia.order.enums;

import lombok.Getter;

/**
 * 订单操作人类型
 */
@Getter
public enum OperatorType {

    CUSTOMER(1, "客户"),
    PROVIDER(2, "服务商"),
    ADMIN(3, "管理员"),
    SYSTEM(4, "系统");

    private final Integer code;
    private final String description;

    OperatorType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
