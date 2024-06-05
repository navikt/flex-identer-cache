package no.nav.helse.flex

import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.IdentType
import no.nav.helse.flex.repository.Identifikator
import no.nav.helse.flex.util.OBJECT_MAPPER
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.OffsetDateTime

class AktorTest : FellesTestOppsett() {
    @Autowired
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Test
    @Disabled
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

        val record = ProducerRecord<String, Any>("pdl.aktor-v2", aktor.aktorId.serialisertTilString())
        kafkaProducerForTest.send(record)
        kafkaProducerForTest.flush()

        aivenAktorConsumer.ventPåRecords(1)

        val aktorRecordFraKafka = aivenAktorConsumer.ventPåRecords(antall = 1, java.time.Duration.ofSeconds(2)).first()
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
