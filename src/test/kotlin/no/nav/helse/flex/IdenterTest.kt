package no.nav.helse.flex

import no.nav.helse.flex.kafka.Ident
import no.nav.helse.flex.kafka.IdentType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.OffsetDateTime

class IdenterTest : FellesTestOppsett() {
    @Autowired
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun `les identer fra topic`() {
        // TODO: les data fra mock topic?

        val ident =
            Ident(
                idnummer = 12345678234.toString(),
                type = IdentType.AKTORID,
                gjeldende = true,
            )
        aivenKafkaProducer.produserMelding(ident)
        val identer = aivenIdenterConsumer.ventPåRecords(antall = 1, java.time.Duration.ofSeconds(2)).tilIdenter()

        identer.forEach {
            assert(it.idnummer == ident.idnummer)
        }

//        identerRepository.lagre(ident.idnummer, OffsetDateTime.now(), ident.type.name, ident.gjeldende)
//        assert(antallIdenterIDb() == 1)
//        Assertions.assertEquals(12345678234.toString(), identerRepository.finnAlleDistinctTags().first())
    }

    @Test
    fun `behandle ident med type FNR`() {
        val ident =
            Ident(
                idnummer = 12345678234.toString(),
                type = IdentType.AKTORID,
                gjeldende = true,
            )
        aktorRepository.lagre(ident.idnummer, OffsetDateTime.now(), ident.type.name, ident.gjeldende)
        assert(antallIdenterIDb() == 1)
    }

    fun antallIdenterIDb(): Int {
        return namedParameterJdbcTemplate.queryForObject(
            """
                SELECT COUNT(1) FROM IDENTER 
            """,
            MapSqlParameterSource(),
            Integer::class.java,
        )!!.toInt()
    }
}
