// booking/cache/SeatAvailabilityCacheService.java
package com.ticketing.booking.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SeatAvailabilityCacheService {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "event:";
    private static final String KEY_SUFFIX = ":seats";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SeatAvailabilityQueryRepository queryRepository;

    public SeatAvailabilityCacheService(
            RedisTemplate<String, Object> redisTemplate,
            SeatAvailabilityQueryRepository queryRepository) {
        this.redisTemplate = redisTemplate;
        this.queryRepository = queryRepository;
    }

    /**
     * Cache-aside read. On a miss (or after eviction), rebuilds from Postgres
     * and repopulates with a fresh TTL.
     */
    @SuppressWarnings("unchecked")
    public List<SeatAvailabilityView> getOrLoad(UUID eventId) {
        String key = cacheKey(eventId);

        List<SeatAvailabilityView> cached =
                (List<SeatAvailabilityView>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        List<SeatAvailabilityView> view = load(eventId);
        redisTemplate.opsForValue().set(key, view, TTL);
        return view;
    }

    /**
     * Invoked after a booking-affecting transaction commits (see
     * BookingSeatsChangedListener). Evicts rather than rewrites the cache
     * entry — the next read repopulates it, which keeps this method free of
     * any race with a concurrent read that might otherwise re-cache stale
     * data mid-invalidation.
     */
    public void evict(UUID eventId) {
        redisTemplate.delete(cacheKey(eventId));
    }

    private List<SeatAvailabilityView> load(UUID eventId) {
        return new ArrayList<>(
                queryRepository.findAvailabilityForEvent(eventId, Instant.now()).stream()
                        .map(SeatAvailabilityView::from)
                        .toList()
        );
    }

    private String cacheKey(UUID eventId) {
        return KEY_PREFIX + eventId + KEY_SUFFIX;
    }
}