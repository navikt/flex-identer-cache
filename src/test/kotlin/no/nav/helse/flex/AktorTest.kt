package no.nav.helse.flex

import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.IdentType
import no.nav.helse.flex.repository.Identifikator
import no.nav.helse.flex.util.toAktor
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.OffsetDateTime

class AktorTest : FellesTestOppsett() {
    @Autowired
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun `les identer fra topic`() {
        // TODO: les data fra mock topic?

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
//        aivenKafkaProducer.produserMelding(aktor)
        val aktorRecord = aivenAktorConsumer.ventPåRecords(antall = 1, java.time.Duration.ofSeconds(2)).first()
        val hentetAktor = aktorRecord.value().toAktor(aktorRecord.key())
        assert(hentetAktor.identifikatorer.first().idnummer == 12345678234.toString())

//        identerRepository.lagre(ident.idnummer, OffsetDateTime.now(), ident.type.name, ident.gjeldende)
//        assert(antallIdenterIDb() == 1)
//        Assertions.assertEquals(12345678234.toString(), identerRepository.finnAlleDistinctTags().first())
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
