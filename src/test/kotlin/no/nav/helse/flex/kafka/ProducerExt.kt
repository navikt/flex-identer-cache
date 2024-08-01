package no.nav.helse.flex.kafka

import no.nav.helse.flex.repository.Aktor
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

fun KafkaProducer<String, GenericRecord>.sendAktor(
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
                identifikatorRecord.put("type", GenericData.EnumSymbol(identifikatorSchema.getField("type").schema(), identifikator.type))
                identifikatorRecord
            }.toList()

    aktorRecord.put("identifikatorer", identifikatorRecords)
    this.send(ProducerRecord(topic, aktor.aktorId, aktorRecord))
}

fun KafkaProducer<String, GenericRecord>.sendMalformedRecord(topic: String) {
    val malformedRecord: GenericRecord = GenericData.Record(Schema.create(Schema.Type.STRING))
    this.send(ProducerRecord(topic, "malformed-key", malformedRecord))
}
