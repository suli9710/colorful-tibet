package com.tibet.tourism.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String IP_LOCATION_CACHE = "ipLocationCache";
    public static final String SPOT_CACHE = "spotCache";
    public static final String POPULAR_SPOTS_CACHE = "popularSpotsCache";
    public static final String DICTIONARY_CACHE = "dictionaryCache";

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache(IP_LOCATION_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(200)
                        .build());
        cacheManager.registerCustomCache(SPOT_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .build());
        cacheManager.registerCustomCache(POPULAR_SPOTS_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .build());
        cacheManager.registerCustomCache(DICTIONARY_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(500)
                        .build());
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}
