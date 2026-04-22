package com.tibet.tourism.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 缓存配置
 * 使用内存缓存（ConcurrentHashMap），适合中小型应用
 * 生产环境可替换为 Redis CacheManager
 */
@Configuration
public class CacheConfig {

    public static final String IP_LOCATION_CACHE = "ipLocationCache";
    public static final String SPOT_CACHE = "spotCache";
    public static final String POPULAR_SPOTS_CACHE = "popularSpotsCache";
    public static final String DICTIONARY_CACHE = "dictionaryCache";

    @Bean
    @Primary
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                IP_LOCATION_CACHE,
                SPOT_CACHE,
                POPULAR_SPOTS_CACHE,
                DICTIONARY_CACHE
        );
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}
