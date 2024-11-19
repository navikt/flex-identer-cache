package no.nav.helse.flex

import no.nav.helse.flex.kafka.ventPaRecords
import no.nav.helse.flex.model.Aktor
import no.nav.helse.flex.model.Identifikator
import no.nav.helse.flex.model.Type
import no.nav.helse.flex.util.osloZone
import no.nav.helse.flex.util.tilOsloZone
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be in range`
import org.amshove.kluent.`should not be`
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class AktorIntegrasjonsTest : FellesTestOppsett() {
    @Test
    fun `les identer fra topic og lagre i cache`() {
        val ident =
            Identifikator(
                idnummer = 12345678234.toString(),
                type = Type.NPID,
                gjeldende = true,
                oppdatert = OffsetDateTime.now(),
            )
        val aktor =
            Aktor(
                aktorId = "\"2286257412903",
                identifikatorer = listOf(ident),
            )

        aktorProducer.sendAktorToTopic(aktor)
        aktorProducer.sendAktorToTopic(aktor)
        kafkaProducerForTest.flush()

        val aktorRecordFraKafka = aktorConsumer.ventPaRecords(antall = 1).first()
        aktorRecordFraKafka.aktorId `should be equal to` 2286257412903.toString()

        aktorService.hentAktor(aktorRecordFraKafka.aktorId).let { aktorFraDb ->
            aktorFraDb `should not be` null
            aktorFraDb?.let { aktor ->
                aktor.identifikatorer.size `should be equal to` 1
                aktor.identifikatorer.first().let { identifikator ->
                    identifikator.type `should be equal to` ident.type
                    identifikator.idnummer `should be equal to` ident.idnummer
                    identifikator.gjeldende `should be equal to` ident.gjeldende
                    ChronoUnit.SECONDS.between(
                        identifikator.oppdatert.tilOsloZone().truncatedTo(ChronoUnit.SECONDS),
                        OffsetDateTime.now(osloZone).truncatedTo(ChronoUnit.SECONDS),
                    ) `should be in range` -10L..10L
                }
            }
        }
    }

    @Test
    fun `håndter nullverdier`() {
        aktorProducer.sendNullVerdi("65432")
        kafkaProducerForTest.flush()

        aktorConsumer.ventPaRecords(antall = 0)
    }

    @Test
    fun `behandle ident med type FNR`() {
        val ident =
            Identifikator(
                idnummer = 12345678234.toString(),
                type = Type.FOLKEREGISTERIDENT,
                gjeldende = true,
                oppdatert = OffsetDateTime.now(),
            )
        val aktor =
            Aktor(
                aktorId = "1456432",
                identifikatorer = listOf(ident),
            )
        aktorService.lagreFlereAktorer(listOf(aktor))
        val lagretIdent = aktorService.hentAktor(aktor.aktorId)?.identifikatorer?.first()
        lagretIdent `should not be` null
        lagretIdent!!.let {
            it.idnummer `should be equal to` it.idnummer
            it.type `should be equal to` ident.type
            it.gjeldende `should be equal to` ident.gjeldende
        }
    }
}
