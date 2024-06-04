package no.nav.helse.flex.config

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import no.nav.helse.flex.logger
import org.apache.avro.Schema
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("test")
@Configuration
class KafkaConfig(
    @Value("\${KAFKA_BROKERS}") private val kafkaBrokers: String,
) {
    val log = logger()
    private final val schemaString = """
   {
  "type": "record",
  "name": "Aktor",
  "namespace": "com.example",
  "fields": [
    {
      "name": "aktorId",
      "type": "string"
    },
    {
      "name": "identifikatorer",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "Identifikator",
          "fields": [
            {
              "name": "idnummer",
              "type": "string"
            },
            {
              "name": "type",
              "type": "string"
            },
            {
              "name": "gjeldende",
              "type": "boolean"
            }
          ]
        }
      }
    }
  ]
}
"""

    @Bean
    @Primary
    fun mockSchemaRegistryClient(): SchemaRegistryClient {
        val client = MockSchemaRegistryClient()
        val schema: Schema = Schema.Parser().parse(schemaString)
        val avroSchema = AvroSchema(schema)
        val subject = "pdl.aktor-v2-value"
        client.register(subject, avroSchema)
        log.info("Schema registered for subject: $subject with schema: $schemaString")
        return client
    }

    @Bean
    fun kafkaAvroSerializer(mockSchemaRegistryClient: SchemaRegistryClient): KafkaAvroSerializer {
        return KafkaAvroSerializer(
            mockSchemaRegistryClient,
            mapOf(
                KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to true,
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "",
            ),
        )
    }

    @Bean
    fun kafkaAvroDeserializer(mockSchemaRegistryClient: SchemaRegistryClient): KafkaAvroDeserializer {
        return KafkaAvroDeserializer(
            mockSchemaRegistryClient,
            mapOf(
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "",
            ),
        )
    }

    @Bean
    fun kafkaProducerForTest(kafkaAvroSerializer: KafkaAvroSerializer): KafkaProducer<String, Any> {
        // TODO set topic pdl.aktor-v2
        val configs =
            mapOf(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.RETRIES_CONFIG to 10,
                ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 100,
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                SaslConfigs.SASL_MECHANISM to "PLAIN",
            )
        log.info("Kafka Producer Config: $configs")

        return KafkaProducer(
            configs,
            StringSerializer(),
            kafkaAvroSerializer,
        )
    }
}
