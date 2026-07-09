package com.bhojanbandhu.platform.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {
    private final StringRedisTemplate redisTemplate;

    public void publish(String channel, String payload) {
        try {
            redisTemplate.convertAndSend(channel, payload);
        } catch (RedisConnectionFailureException ignored) {
            // Local development can run without Redis; production should alert on this.
        }
    }
}
