package no.nav.helse.flex.kafka

import no.nav.helse.flex.logger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer

@Configuration
class AivenKafkaConfig(
    @Value("\${KAFKA_BROKERS}") private val kafkaBrokers: String,
    @Value("\${KAFKA_SCHEMA_REGISTRY}") val schemaRegistryUrl: String,
    @Value("\${KAFKA_SCHEMA_REGISTRY_USER}") private val schemaRegistryUser: String,
    @Value("\${KAFKA_SCHEMA_REGISTRY_PASSWORD}") private val schemaRegistryPassword: String,
    @Value("\${KAFKA_SECURITY_PROTOCOL:SSL}") private val kafkaSecurityProtocol: String,
    @Value("\${KAFKA_TRUSTSTORE_PATH}") private val kafkaTruststorePath: String,
    @Value("\${KAFKA_CREDSTORE_PASSWORD}") private val kafkaCredstorePassword: String,
    @Value("\${KAFKA_KEYSTORE_PATH}") private val kafkaKeystorePath: String,
) {
    val log = logger()

    fun commonConfig() =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
        ) + securityConfig()

    private fun securityConfig() =
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to kafkaSecurityProtocol,
            // Disables server host name verification.
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
        )

    @Bean
    fun consumerFactory(): ConsumerFactory<String, ByteArray> {
        log.info("Oppretter bytearray consumer config")
        val props =
            HashMap<String, Any>(
                mapOf(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                    ConsumerConfig.GROUP_ID_CONFIG to "flex-aktor-dev-v13",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
                    ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java.name,
                    ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to ByteArrayDeserializer::class.java.name,
                    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "600000",
                    ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to "30000",
                    ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to "3000",
                    ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG to 30000,
                    ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG to 300000,
                ),
            )

        return DefaultKafkaConsumerFactory(props + commonConfig())
    }

    @Bean
    fun kafkaAvroListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, ByteArray>,
        aivenKafkaErrorHandler: AivenKafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, ByteArray> {
        log.info("Oppretter kafka bytearray listener factory")
        val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray>()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setCommonErrorHandler(aivenKafkaErrorHandler)
        return factory
    }
}

const val AKTOR_TOPIC = "pdl.aktor-v2"
