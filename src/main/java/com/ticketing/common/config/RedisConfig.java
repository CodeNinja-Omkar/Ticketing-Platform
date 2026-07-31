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
import tools.jackson.databind.DefaultTyping;


@Configuration
public class RedisConfig {

    /*
    "NON_FINAL_AND_RECORDS types records themselves but, empirically, also requires each
    non-natural final field type they contain to be individually allowlisted"
     */

    /**
     * Values are JSON via GenericJacksonJsonRedisSerializer - Spring Data
     * Redis's Jackson 3-based serializer, replacing the deprecated
     * GenericJackson2JsonRedisSerializer as of Spring Data Redis 4.0.
     *
     * Typing policy is DefaultTyping.NON_FINAL_AND_RECORDS (Jackson 3.1+),
     * not the builder's default NON_FINAL. Reason: NON_FINAL skips embedding
     * type-id metadata for any value whose *runtime* class is final. Records
     * (e.g. SeatAvailabilityView) are implicitly final, so under plain
     * NON_FINAL they were silently written without a type wrapper - but
     * RedisTemplate<String, Object> always deserializes through the
     * type-erased Object path, which unconditionally expects that wrapper on
     * read. The result was a write/read asymmetry that only surfaced as a
     * SerializationException on a genuine cache HIT (a fresh MISS-then-write
     * never exercises the read path), which is why this wasn't caught until
     * an integration test specifically re-read an already-cached value.
     * NON_FINAL_AND_RECORDS (added in Jackson 3.1 for exactly this problem)
     * patches the record case specifically, the same way NON_FINAL_AND_ENUMS
     * patches enums - it does not make the finality exclusion disappear for
     * other final types.
     *
     * Two other final types besides the record tripped the same exclusion
     * and needed explicit handling:
     *   - Stream.toList() returns an immutable, final JDK list implementation,
     *     not ArrayList - the *container*, not just its elements, was being
     *     silently excluded from typing. Fixed at the call site
     *     (SeatAvailabilityCacheService.load()) by wrapping the result in an
     *     explicit `new ArrayList<>(...)` before caching.
     *   - Individual final field types inside the record - BigDecimal, UUID -
     *     were, empirically, ALSO excluded despite being concretely typed
     *     record components. This was unexpected given how default typing
     *     normally treats POJO fields with known declared types, and isn't
     *     fully explained by public Jackson documentation at this Jackson 3.1
     *     baseline - treat as an observed constraint, not a designed one.
     *
     * Practical consequence for future changes: if SeatAvailabilityView (or
     * any other type cached through this template) gains a new field whose
     * type is a concrete final class that isn't one of Jackson's exempted
     * "natural" types (String, Boolean, numeric wrapper types) or a record,
     * expect a similar InvalidTypeIdException on the first cache-hit test
     * that reads it back - and expect to need BOTH a NON_FINAL_AND_RECORDS-style
     * typing fix (if it's a new record) AND an explicit BasicPolymorphicTypeValidator
     * allowlist entry for that exact class.
     *
     * The allowlist itself is scoped to com.ticketing (this application's own
     * types) plus the specific JDK types actually observed in cached
     * payloads (ArrayList, BigDecimal, UUID) - deliberately not a broad
     * java.util./java.math. prefix, to keep the allowlist matched to what
     * this cache actually produces rather than permitting arbitrary JDK
     * deserialization targets.
     */

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ticketing.")
                .allowIfSubType(java.util.ArrayList.class)
                .allowIfSubType(java.math.BigDecimal.class)
                .allowIfSubType(java.util.UUID.class)
                .build();

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .customize(builder -> builder.activateDefaultTyping(typeValidator, DefaultTyping.NON_FINAL_AND_RECORDS))
                .build();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}