package com.tibet.tourism.modules.recommendation.application.strategy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.security.CacheKeyHasher;
import com.tibet.tourism.modules.recommendation.application.RecommendationLogPrivacy;
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
    private static final String USER_CACHE_NAMESPACE = "user";
    private static final String SIMILAR_USER_CACHE_NAMESPACE = "similar-user";

    private static final TypeReference<Map<String, Double>> STRING_DOUBLE_MAP = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheKeyHasher cacheKeyHasher;

    private final Map<String, Map<String, Double>> localSimilarityCache = Collections.synchronizedMap(
            new LinkedHashMap<String, Map<String, Double>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Map<String, Double>> eldest) {
                    return size() > LOCAL_CACHE_SIZE_LIMIT;
                }
            });

    private final Map<String, Map<String, Double>> localTagProfileCache = Collections.synchronizedMap(
            new LinkedHashMap<String, Map<String, Double>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Map<String, Double>> eldest) {
                    return size() > LOCAL_CACHE_SIZE_LIMIT;
                }
            });

    private volatile boolean redisCacheWarningLogged = false;

    public RecommendationCacheService(@Autowired(required = false) StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper,
                                      CacheKeyHasher cacheKeyHasher) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheKeyHasher = cacheKeyHasher;
    }

    public Map<String, Double> getTagProfile(Long userId) {
        String userCacheKey = userCacheKey(userId);
        Map<String, Double> redisValue = toStringDoubleMap(getRedisValue(tagProfileKey(userCacheKey), "get tag profile"));
        return redisValue != null ? redisValue : localTagProfileCache.get(userCacheKey);
    }

    public Map<String, Double> getSimilarity(Long userId) {
        String userCacheKey = userCacheKey(userId);
        Map<String, Double> redisValue = toStringDoubleMap(getRedisValue(similarityKey(userCacheKey), "get similarity"));
        return redisValue != null ? redisValue : localSimilarityCache.get(userCacheKey);
    }

    public void cacheTagProfile(Long userId, Map<String, Double> tagProfile) {
        String userCacheKey = userCacheKey(userId);
        localTagProfileCache.put(userCacheKey, new HashMap<>(tagProfile));
        setRedisValue(tagProfileKey(userCacheKey), tagProfile, TAG_PROFILE_CACHE_TTL_MINUTES, "set tag profile");
    }

    public void cacheSimilarity(Long userId, Map<Long, Double> similarities) {
        String userCacheKey = userCacheKey(userId);
        Map<String, Double> minimizedSimilarities = toMinimizedSimilarityMap(similarities);
        localSimilarityCache.put(userCacheKey, minimizedSimilarities);
        setRedisValue(similarityKey(userCacheKey), minimizedSimilarities, SIMILARITY_CACHE_TTL_MINUTES, "set similarity");
    }

    public void invalidateUserCache(Long userId) {
        String userCacheKey = userCacheKey(userId);
        localSimilarityCache.remove(userCacheKey);
        localTagProfileCache.remove(userCacheKey);
        deleteRedisValue(similarityKey(userCacheKey));
        deleteRedisValue(tagProfileKey(userCacheKey));
    }

    String similarUserCacheKey(Long userId) {
        return cacheKeyHasher.cacheKey(SIMILAR_USER_CACHE_NAMESPACE, userId);
    }

    private String userCacheKey(Long userId) {
        return cacheKeyHasher.cacheKey(USER_CACHE_NAMESPACE, userId);
    }

    private Map<String, Double> toMinimizedSimilarityMap(Map<Long, Double> similarities) {
        Map<String, Double> minimized = new HashMap<>();
        if (similarities == null) {
            return minimized;
        }
        for (Map.Entry<Long, Double> entry : similarities.entrySet()) {
            Double score = toDouble(entry.getValue());
            if (entry.getKey() != null && score != null) {
                minimized.put(similarUserCacheKey(entry.getKey()), score);
            }
        }
        return minimized;
    }

    private String similarityKey(String userCacheKey) {
        return "recommend:similarity:" + userCacheKey;
    }

    private String tagProfileKey(String userCacheKey) {
        return "recommend:tagprofile:" + userCacheKey;
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
            logger.warn("Redis cache unavailable; using local in-memory cache. operation={}, error={}",
                    operation, RecommendationLogPrivacy.exceptionSummary(ex));
        } else {
            logger.debug("Redis cache operation failed: operation={}, error={}",
                    operation, RecommendationLogPrivacy.exceptionSummary(ex));
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

    private Double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String text) {
            try { return Double.parseDouble(text); } catch (NumberFormatException ex) { return null; }
        }
        return null;
    }
}
