package com.daijia.infra;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.redis")
public class AppRedisProperties {

    /**
     * 是否启用 Redis。
     * 关闭或 Redis 不可用时会自动降级：幂等与并发控制由数据库唯一约束与乐观锁兜底。
     */
    private boolean enabled = true;
}
