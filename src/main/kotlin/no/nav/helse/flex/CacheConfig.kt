package no.nav.helse.flex

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.net.URI

@Configuration
class CacheConfig(
    @Value("\${REDIS_URI_SESSIONS}") val redisUriString: String,
    @Value("\${REDIS_USERNAME_SESSIONS}") val redisUsername: String,
    @Value("\${REDIS_PASSWORD_SESSIONS}") val redisPassword: String,
) {
    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val redisUri = URI.create(redisUriString)
        val redisConnection = RedisStandaloneConfiguration(redisUri.host, redisUri.port)

        redisConnection.username = redisUsername
        redisConnection.password = RedisPassword.of(redisPassword)

        val clientConfiguration =
            LettuceClientConfiguration.builder().apply {
                if ("default" != redisUsername) {
                    useSsl()
                }
            }.build()

        return LettuceConnectionFactory(redisConnection, clientConfiguration)
    }

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val cacheConfigurations: MutableMap<String, RedisCacheConfiguration> = HashMap()

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            .withInitialCacheConfigurations(cacheConfigurations)
            .enableStatistics()
            .build()
    }

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = redisConnectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer() // You can customize serializers as needed
        return template
    }
}