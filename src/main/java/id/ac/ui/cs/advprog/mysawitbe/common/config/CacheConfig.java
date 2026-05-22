package id.ac.ui.cs.advprog.mysawitbe.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableCaching
public class CacheConfig {

        private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @SuppressWarnings("removal")
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Required so cached values deserialize back to their original types.
                // Without type metadata, GenericJackson2JsonRedisSerializer will read JSON into
                // Maps/Lists (e.g., LinkedHashMap) and @Cacheable will fail with ClassCastException.
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.EVERYTHING,
                        JsonTypeInfo.As.PROPERTY
                );

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        "wallet-balance",    defaults.entryTtl(Duration.ofSeconds(30)),
                        "wallet-tx",         defaults.entryTtl(Duration.ofSeconds(30)),
                        "payroll-status",    defaults.entryTtl(Duration.ofSeconds(10))
                ))
                .build();
    }

    // Clears all caches on startup so stale entries from a previous classloader
    // (DevTools RestartClassLoader) never cause ClassCastException on deserialize.
    @Bean
    public ApplicationListener<ApplicationReadyEvent> cacheEvictor(CacheManager cacheManager) {
        return event -> cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

        // Under load tests we prefer correctness/availability over caching.
        // If Redis is down or a cache entry can't be deserialized, treat it as a cache miss.
        @Bean
        public CacheErrorHandler cacheErrorHandler() {
                return new CacheErrorHandler() {
                        @Override
                        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                                log.warn("Cache GET error (cacheName={}, key={}): {}", cache.getName(), key, exception.toString());
                        }

                        @Override
                        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                                log.warn("Cache PUT error (cacheName={}, key={}): {}", cache.getName(), key, exception.toString());
                        }

                        @Override
                        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                                log.warn("Cache EVICT error (cacheName={}, key={}): {}", cache.getName(), key, exception.toString());
                        }

                        @Override
                        public void handleCacheClearError(RuntimeException exception, Cache cache) {
                                log.warn("Cache CLEAR error (cacheName={}): {}", cache.getName(), exception.toString());
                        }
                };
        }
}
