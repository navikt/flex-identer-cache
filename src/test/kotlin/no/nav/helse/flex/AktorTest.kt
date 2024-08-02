package no.nav.helse.flex

import no.nav.helse.flex.kafka.AKTOR_TOPIC
import no.nav.helse.flex.kafka.sendAktor
import no.nav.helse.flex.kafka.ventPaRecords
import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.IdentType
import no.nav.helse.flex.repository.Identifikator
import no.nav.helse.flex.util.osloZone
import no.nav.helse.flex.util.tilOsloZone
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be in range`
import org.amshove.kluent.`should not be`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class AktorTest : FellesTestOppsett() {
    @Autowired
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun `les identer fra topic`() {
        val ident =
            Identifikator(
                idnummer = 12345678234.toString(),
                type = IdentType.NPID.name,
                gjeldende = true,
//                oppdatert = OffsetDateTime.now(),
            )
        val aktor =
            Aktor(
                aktorId = "\"2286257412903",
                identifikatorer = listOf(ident),
            )

        kafkaProducerForTest.sendAktor(AKTOR_TOPIC, aktor)
        kafkaProducerForTest.flush()

        val aktorRecordFraKafka = aktorConsumer.ventPaRecords(antall = 1).first()
        aktorRecordFraKafka.aktorId `should be equal to` 2286257412903.toString()

        // / Sjekker at aktøren blir lagret i db
        aktorRepository.findByAktorId(aktorRecordFraKafka.aktorId).let { aktorFraDb ->
            aktorFraDb `should not be` null
            aktorFraDb?.let { aktor ->
                aktor.identifikatorer.size `should be equal to` 1
                aktor.identifikatorer.first().let { identifikator ->
                    identifikator.type `should be equal to` ident.type
                    identifikator.idnummer `should be equal to` ident.idnummer
                    identifikator.gjeldende `should be equal to` ident.gjeldende
                    ChronoUnit.SECONDS.between(
                        identifikator.oppdatert?.tilOsloZone()?.truncatedTo(ChronoUnit.SECONDS),
                        OffsetDateTime.now(osloZone).truncatedTo(ChronoUnit.SECONDS),
                    ) `should be in range` -10L..10L
                }
            }
        }
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
                SELECT COUNT(*) FROM identifikator 
            """,
            MapSqlParameterSource(),
            Integer::class.java,
        )!!.toInt()
    }
}
