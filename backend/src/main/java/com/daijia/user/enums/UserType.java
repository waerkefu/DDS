package com.daijia.user.enums;

import lombok.Getter;

/**
 * 用户类型
 */
@Getter
public enum UserType {

    CUSTOMER(1, "客户"),
    PROVIDER(2, "服务商"),
    ADMIN(3, "管理员");

    private final Integer code;
    private final String description;

    UserType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserType of(Integer code) {
        for (UserType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
