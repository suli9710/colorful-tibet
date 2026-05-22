package com.tibet.tourism.modules.recommendation.application.strategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationCacheService.class);

    private static final long SIMILARITY_CACHE_TTL_MINUTES = 30;
    private static final long TAG_PROFILE_CACHE_TTL_MINUTES = 60;
    private static final int LOCAL_CACHE_SIZE_LIMIT = 1000;

    private static final TypeReference<Map<String, Double>> STRING_DOUBLE_MAP = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final Map<Long, Map<Long, Double>> localSimilarityCache = Collections.synchronizedMap(
            new LinkedHashMap<Long, Map<Long, Double>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Map<Long, Double>> eldest) {
                    return size() > LOCAL_CACHE_SIZE_LIMIT;
                }
            });

    private final Map<Long, Map<String, Double>> localTagProfileCache = Collections.synchronizedMap(
            new LinkedHashMap<Long, Map<String, Double>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Map<String, Double>> eldest) {
                    return size() > LOCAL_CACHE_SIZE_LIMIT;
                }
            });

    private volatile boolean redisCacheWarningLogged = false;

    public RecommendationCacheService(@Autowired(required = false) StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Double> getTagProfile(Long userId) {
        Map<String, Double> redisValue = toStringDoubleMap(getRedisValue(tagProfileKey(userId), "get tag profile"));
        return redisValue != null ? redisValue : localTagProfileCache.get(userId);
    }

    public Map<Long, Double> getSimilarity(Long userId) {
        Map<Long, Double> redisValue = toLongDoubleMap(getRedisValue(similarityKey(userId), "get similarity"));
        return redisValue != null ? redisValue : localSimilarityCache.get(userId);
    }

    public void cacheTagProfile(Long userId, Map<String, Double> tagProfile) {
        localTagProfileCache.put(userId, new HashMap<>(tagProfile));
        setRedisValue(tagProfileKey(userId), tagProfile, TAG_PROFILE_CACHE_TTL_MINUTES, "set tag profile");
    }

    public void cacheSimilarity(Long userId, Map<Long, Double> similarities) {
        localSimilarityCache.put(userId, new HashMap<>(similarities));
        setRedisValue(similarityKey(userId), new HashMap<>(similarities), SIMILARITY_CACHE_TTL_MINUTES, "set similarity");
    }

    public void invalidateUserCache(Long userId) {
        localSimilarityCache.remove(userId);
        localTagProfileCache.remove(userId);
        deleteRedisValue(similarityKey(userId));
        deleteRedisValue(tagProfileKey(userId));
    }

    private static String similarityKey(Long userId) {
        return "recommend:similarity:" + userId;
    }

    private static String tagProfileKey(Long userId) {
        return "recommend:tagprofile:" + userId;
    }

    private String getRedisValue(String key, String operation) {
        if (redisTemplate == null) return null;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            logRedisCacheFailure(operation, ex);
            return null;
        }
    }

    private void setRedisValue(String key, Object value, long ttlMinutes, String operation) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttlMinutes, TimeUnit.MINUTES);
        } catch (RuntimeException ex) {
            logRedisCacheFailure(operation, ex);
        } catch (Exception ex) {
            logRedisCacheFailure(operation, new IllegalStateException("Failed to serialize recommendation cache", ex));
        }
    }

    private void deleteRedisValue(String key) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            logRedisCacheFailure("delete cache", ex);
        }
    }

    private void logRedisCacheFailure(String operation, RuntimeException ex) {
        if (!redisCacheWarningLogged) {
            redisCacheWarningLogged = true;
            logger.warn("Redis cache unavailable; using local in-memory cache. operation={}, cause={}",
                    operation, ex.getMessage());
        } else {
            logger.debug("Redis cache operation failed: {}", operation, ex);
        }
    }

    private Map<String, Double> toStringDoubleMap(String value) {
        if (value == null || value.isBlank()) return null;
        Map<String, Double> source;
        try {
            source = objectMapper.readValue(value, STRING_DOUBLE_MAP);
        } catch (Exception ex) {
            logRedisCacheFailure("parse tag profile", new IllegalStateException("Invalid recommendation cache payload", ex));
            return null;
        }
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            Double score = toDouble(entry.getValue());
            if (entry.getKey() != null && score != null) {
                result.put(entry.getKey(), score);
            }
        }
        return result;
    }

    private Map<Long, Double> toLongDoubleMap(String value) {
        if (value == null || value.isBlank()) return null;
        Map<String, Double> source;
        try {
            source = objectMapper.readValue(value, STRING_DOUBLE_MAP);
        } catch (Exception ex) {
            logRedisCacheFailure("parse similarity", new IllegalStateException("Invalid recommendation cache payload", ex));
            return null;
        }
        Map<Long, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            Long id = toLong(entry.getKey());
            Double score = toDouble(entry.getValue());
            if (id != null && score != null) {
                result.put(id, score);
            }
        }
        return result;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text) {
            try { return Long.parseLong(text); } catch (NumberFormatException ex) { return null; }
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String text) {
            try { return Double.parseDouble(text); } catch (NumberFormatException ex) { return null; }
        }
        return null;
    }
}
