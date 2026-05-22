package com.tibet.tourism.common.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class CacheConfig implements CachingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    public static final String IP_LOCATION_CACHE = "ipLocationCache";
    public static final String SPOT_CACHE = "spotCache";
    public static final String POPULAR_SPOTS_CACHE = "popularSpotsCache";
    public static final String NEWS_CACHE = "newsCache";
    public static final String DICTIONARY_CACHE = "dictionaryCache";

    @Bean
    @Primary
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> connectionFactoryProvider) {
        RedisConnectionFactory connectionFactory = connectionFactoryProvider.getIfAvailable();
        if (connectionFactory == null) {
            return fallbackCacheManager();
        }

        RedisCacheConfiguration defaultConfig = redisCacheConfiguration(Duration.ofHours(1));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(IP_LOCATION_CACHE, redisCacheConfiguration(Duration.ofHours(24)));
        cacheConfigurations.put(SPOT_CACHE, redisCacheConfiguration(Duration.ofMinutes(30)));
        cacheConfigurations.put(POPULAR_SPOTS_CACHE, redisCacheConfiguration(Duration.ofMinutes(15)));
        cacheConfigurations.put(NEWS_CACHE, redisCacheConfiguration(Duration.ofMinutes(15)));
        cacheConfigurations.put(DICTIONARY_CACHE, redisCacheConfiguration(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    private CacheManager fallbackCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                IP_LOCATION_CACHE,
                SPOT_CACHE,
                POPULAR_SPOTS_CACHE,
                NEWS_CACHE,
                DICTIONARY_CACHE
        );
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logCacheError("get", exception, cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logCacheError("put", exception, cache, key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logCacheError("evict", exception, cache, key);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logCacheError("clear", exception, cache, null);
            }
        };
    }

    private RedisCacheConfiguration redisCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .prefixCacheNameWith("colorful-tibet:cache:")
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(new StringRedisSerializer()));
    }

    private void logCacheError(String operation, RuntimeException exception, Cache cache, Object key) {
        String cacheName = cache == null ? "unknown" : cache.getName();
        logger.warn("Redis cache {} failed, fallback to method execution. cache={}, key={}, cause={}",
                operation, cacheName, key, exception.getMessage());
        logger.debug("Redis cache error details", exception);
    }
}
