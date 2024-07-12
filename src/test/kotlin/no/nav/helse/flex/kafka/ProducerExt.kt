package no.nav.helse.flex.kafka

import no.nav.helse.flex.repository.Aktor
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.EncoderFactory
import org.apache.avro.io.JsonEncoder
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.ByteArrayOutputStream

fun KafkaProducer<String, String>.produceAvroRecord(aktor: Aktor): ProducerRecord<String, String> {
    val schema: Schema =
        Schema.Parser().parse(
            this::class.java.classLoader.getResourceAsStream("avro/aktor.avsc"),
        )

    val identifikatorer: MutableList<GenericRecord> = mutableListOf()
    for (ident in aktor.identifikatorer) {
        val identifikatorSchema = schema.getField("identifikatorer").schema().elementType
        val identifikator: GenericRecord = GenericData.Record(schema.getField("identifikatorer").schema().elementType)
        identifikator.put("idnummer", ident.idnummer)
        identifikator.put("type", GenericData.EnumSymbol(identifikatorSchema.getField("type").schema(), ident.type))
        identifikator.put("gjeldende", ident.gjeldende)
        identifikatorer.add(identifikator)
    }

    val avroAktor: GenericRecord = GenericData.Record(schema)
    avroAktor.put("identifikatorer", identifikatorer)

    val stream = ByteArrayOutputStream()
    val datumWriter = GenericDatumWriter<GenericRecord>(schema)
    val encoder: JsonEncoder = EncoderFactory.get().jsonEncoder(schema, stream)
    datumWriter.write(avroAktor, encoder)
    encoder.flush()

    return ProducerRecord(AKTOR_TOPIC, aktor.aktorId, stream.toString("UTF-8"))
}
