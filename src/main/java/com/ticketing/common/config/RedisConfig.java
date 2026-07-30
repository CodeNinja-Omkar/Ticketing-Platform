// common/config/RedisConfig.java
package com.ticketing.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
public class RedisConfig {

    /**
     * Values are JSON via GenericJacksonJsonRedisSerializer (Jackson 3-based,
     * Spring Data Redis's replacement for the deprecated
     * GenericJackson2JsonRedisSerializer as of Spring Data Redis 4.0).

     * Default typing is enabled via an explicit allowlist rather than
     * enableUnsafeDefaultTyping(): only types under com.ticketing are
     * permitted to be deserialized as polymorphic values. This is narrower
     * than "trust everything" and costs nothing extra to maintain, since
     * every type this cache will ever store is one we wrote.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ticketing.")
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}