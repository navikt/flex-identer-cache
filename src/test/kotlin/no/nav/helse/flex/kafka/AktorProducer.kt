package no.nav.helse.flex.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.Aktor
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component

@Component
class AktorProducer(
    private val kafkaProducer: KafkaProducer<String, GenericRecord>,
    private val schemaRegistryClient: SchemaRegistryClient,
) {
    val log = logger()

    fun sendAktorToTopic(aktor: Aktor) {
        printSchemaForSubject(schemaRegistryClient, "Aktor-value")
        sendAktor(AKTOR_TOPIC, aktor)
    }

    fun printSchemaForSubject(
        schemaRegistryClient: SchemaRegistryClient,
        subject: String,
    ) {
        log.info(schemaRegistryClient.allSubjects.toString())
        try {
            val schema = schemaRegistryClient.getLatestSchemaMetadata(subject).schema
            log.info("Schema for $subject: $schema")
        } catch (e: Exception) {
            log.error("Error retrieving schema for subject $subject", e)
        }
    }

    fun sendAktor(
        topic: String,
        aktor: Aktor,
    ) {
        val schema: Schema =
            Schema.Parser().parse(
                this::class.java.classLoader.getResourceAsStream("avro/aktor.avsc"),
            )
        val aktorRecord = GenericData.Record(schema)
        val identifikatorRecords: List<GenericRecord> =
            aktor.identifikatorer.stream()
                .map { identifikator ->
                    val identifikatorSchema = schema.getField("identifikatorer").schema().elementType
                    val identifikatorRecord: GenericRecord =
                        GenericData.Record(identifikatorSchema)
                    identifikatorRecord.put("idnummer", identifikator.idnummer)
                    identifikatorRecord.put("gjeldende", identifikator.gjeldende)
                    identifikatorRecord.put(
                        "type",
                        GenericData.EnumSymbol(identifikatorSchema.getField("type").schema(), identifikator.type),
                    )
                    identifikatorRecord
                }.toList()

        aktorRecord.put("identifikatorer", identifikatorRecords)
        kafkaProducer.send(ProducerRecord(topic, aktor.aktorId, aktorRecord))
    }
}
