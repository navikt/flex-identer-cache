package no.nav.helse.flex

import no.nav.helse.flex.kafka.AKTOR_TOPIC
import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.IdentType
import no.nav.helse.flex.repository.Identifikator
import no.nav.helse.flex.util.OBJECT_MAPPER
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.EncoderFactory
import org.apache.avro.io.JsonEncoder
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime

class AktorTest : FellesTestOppsett() {
    @Autowired
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    fun produceAvroRecord(aktor: Aktor): ProducerRecord<String, String> {
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

    @Test
    fun `les identer fra topic`() {
        val ident =
            Identifikator(
                idnummer = 12345678234.toString(),
                type = IdentType.NPID.name,
                gjeldende = true,
                oppdatert = OffsetDateTime.now(),
            )
        val aktor =
            Aktor(
                aktorId = "1345676",
                identifikatorer = listOf(ident),
            )

        val record = produceAvroRecord(aktor)
        kafkaProducerForTest.send(record)
        kafkaProducerForTest.flush()

        aivenAktorConsumer.ventPaRecords(1)

        val aktorRecordFraKafka = aivenAktorConsumer.ventPaRecords(antall = 1, java.time.Duration.ofSeconds(2)).first()
        val hentetAktor = OBJECT_MAPPER.convertValue(aktorRecordFraKafka, Aktor::class.java)
        Assertions.assertEquals(12345678234.toString(), hentetAktor.identifikatorer.first().idnummer)
    }

    @Test
    fun `behandle ident med type FNR`() {
        val ident =
            Identifikator(
                idnummer = 12345678234.toString(),
                type = IdentType.FOLKEREGISTERIDENT.name,
                gjeldende = true,
                oppdatert = OffsetDateTime.now(),
            )
        val aktor =
            Aktor(
                aktorId = "1456432",
                identifikatorer = listOf(ident),
            )
        aktorService.lagreAktor(aktor)
        assert(antallAktorerIDb() == 1)
    }

    fun antallAktorerIDb(): Int {
        return namedParameterJdbcTemplate.queryForObject(
            """
                SELECT COUNT(1) FROM identifikator 
            """,
            MapSqlParameterSource(),
            Integer::class.java,
        )!!.toInt()
    }
}
