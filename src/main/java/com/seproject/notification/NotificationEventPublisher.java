package com.seproject.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private static final String STREAM_KEY = "notification-stream";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(NotificationEventDto event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            stringRedisTemplate.opsForStream().add(STREAM_KEY, Map.of("event", payload));
        } catch (Exception e) {
            log.error("알림 이벤트 발행 실패 - type: {}", event.getType(), e);
        }
    }
}
