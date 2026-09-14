package com.daijia.security;

import com.daijia.common.BusinessException;
import com.daijia.common.ResultCode;

/**
 * 登录用户上下文（基于 ThreadLocal）
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long currentUserId() {
        return require().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
