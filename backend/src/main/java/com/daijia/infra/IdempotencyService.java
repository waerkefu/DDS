package com.daijia.infra;

import com.daijia.common.BusinessException;
import com.daijia.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 幂等控制。
 *
 * <p>Redis 命中则直接判定为重复请求；Redis 未启用/不可用时放行，
 * 由数据库唯一约束（如 payment.idempotency_key、orders.order_no）做最终兜底。</p>
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String KEY_PREFIX = "daijia:idem:";

    private final RedisHelper redisHelper;

    /**
     * 获取幂等令牌。
     *
     * @return true 表示首次请求，可继续处理
     */
    public boolean tryAcquire(String idempotencyKey, Duration ttl) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }
        return redisHelper.setIfAbsent(KEY_PREFIX + idempotencyKey, "1", ttl);
    }

    /**
     * 获取幂等令牌，若为重复请求则抛出业务异常
     */
    public void acquireOrThrow(String idempotencyKey, Duration ttl) {
        if (!tryAcquire(idempotencyKey, ttl)) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST);
        }
    }

    public void release(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        redisHelper.delete(KEY_PREFIX + idempotencyKey);
    }
}
