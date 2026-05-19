package id.ac.ui.cs.advprog.mysawitbe.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableCaching
public class CacheConfig {

    @SuppressWarnings("removal")
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
}
