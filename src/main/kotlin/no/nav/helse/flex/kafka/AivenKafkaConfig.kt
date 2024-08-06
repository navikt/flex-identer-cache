package no.nav.helse.flex.kafka

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.helse.flex.logger
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

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
    fun aivenSchemaRegistryClient(): SchemaRegistryClient {
        return CachedSchemaRegistryClient(
            schemaRegistryUrl,
            20,
            mapOf(
                KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
                KafkaAvroDeserializerConfig.USER_INFO_CONFIG to "$schemaRegistryUser:$schemaRegistryPassword",
            ),
        )
    }

    @Bean
    fun kafkaAvroListenerContainerFactory(
        aivenSchemaRegistryClient: SchemaRegistryClient,
        aivenKafkaErrorHandler: AivenKafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, GenericRecord> {
        val genericAvroConsumerConfig =
            commonConfig() +
                mapOf(
                    KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
                    KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to false,
                    ConsumerConfig.GROUP_ID_CONFIG to "sykepengesoknad-backend",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "none",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
                    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "600000",
                )

        val consumerFactory =
            DefaultKafkaConsumerFactory(
                genericAvroConsumerConfig,
                StringDeserializer(),
                KafkaAvroDeserializer(aivenSchemaRegistryClient),
            )

        val factory = ConcurrentKafkaListenerContainerFactory<String, GenericRecord>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setCommonErrorHandler(aivenKafkaErrorHandler)
        factory.consumerFactory = consumerFactory
        return factory
    }
}

const val AKTOR_TOPIC = "pdl.aktor-v2"
