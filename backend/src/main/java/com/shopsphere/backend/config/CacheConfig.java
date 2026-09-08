package com.shopsphere.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis cache configuration for the catalog. Defines cache names and TTLs,
 * and builds a {@link CacheManager} with per-cache expiration. Cache keys
 * include all relevant query parameters to prevent collisions.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATEGORIES_CACHE = "categories";
    public static final String CATEGORY_CACHE = "category";
    public static final String PRODUCTS_CACHE = "products";
    public static final String PRODUCT_CACHE = "product";

    @Bean
    @ConfigurationProperties(prefix = "app.cache")
    public CacheProperties cacheProperties() {
        return new CacheProperties();
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        CacheProperties props = cacheProperties();

        RedisCacheConfiguration categoryConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(props.getCategoryTtlSeconds()))
                .disableCachingNullValues();

        RedisCacheConfiguration productConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(props.getProductTtlSeconds()))
                .disableCachingNullValues();

        RedisCacheConfiguration productListConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(props.getProductListTtlSeconds()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues())
                .withCacheConfiguration(CATEGORIES_CACHE, categoryConfig)
                .withCacheConfiguration(CATEGORY_CACHE, categoryConfig)
                .withCacheConfiguration(PRODUCTS_CACHE, productListConfig)
                .withCacheConfiguration(PRODUCT_CACHE, productConfig)
                .build();
    }

    /**
     * Holds cache-related configuration from app.cache.* properties.
     */
    public static class CacheProperties {
        private long categoryTtlSeconds = 300;
        private long productTtlSeconds = 300;
        private long productListTtlSeconds = 60;
        private int maxPageSize = 100;

        public long getCategoryTtlSeconds() { return categoryTtlSeconds; }
        public void setCategoryTtlSeconds(long categoryTtlSeconds) { this.categoryTtlSeconds = categoryTtlSeconds; }
        public long getProductTtlSeconds() { return productTtlSeconds; }
        public void setProductTtlSeconds(long productTtlSeconds) { this.productTtlSeconds = productTtlSeconds; }
        public long getProductListTtlSeconds() { return productListTtlSeconds; }
        public void setProductListTtlSeconds(long productListTtlSeconds) { this.productListTtlSeconds = productListTtlSeconds; }
        public int getMaxPageSize() { return maxPageSize; }
        public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }
    }
}