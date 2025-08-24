package com.melly.authjwt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * RedisTemplate Bean 설정
     * Redis와의 데이터 입출력을 위해 key/value 직렬화 방식을 설정합니다.
     * StringRedisSerializer: key를 문자열로 직렬화
     * GenericJackson2JsonRedisSerializer: value를 JSON으로 직렬화/역직렬화
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // RedisConnectionFactory 설정
        template.setConnectionFactory(connectionFactory);

        // Key 직렬화 설정 (String)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 직렬화 설정 (JSON, LocalDateTime 포함)
        template.setValueSerializer(genericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(genericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * LocalDateTime 직렬화 지원 GenericJackson2JsonRedisSerializer 생성
     */
    private GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 지원
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 형태로 저장
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
