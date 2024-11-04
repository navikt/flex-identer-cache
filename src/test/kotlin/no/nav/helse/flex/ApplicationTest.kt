package no.nav.helse.flex

import no.nav.helse.flex.kafka.ventPaRecords
import no.nav.helse.flex.model.Aktor
import no.nav.helse.flex.model.Identifikator
import no.nav.helse.flex.model.Type
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.OffsetDateTime

class ApplicationTest : FellesTestOppsett() {
    @Test
    fun sjekkReadiness() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/health/readiness"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }

    @Test
    fun sjekkLiveness() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/health/liveness"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }

    @Test
    fun isReadyEtterHeleTopicetErLest() {
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

        // Ikke klar før siste offset er lest
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/health/kafka"))
            .andExpect(MockMvcResultMatchers.status().isServiceUnavailable)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("DOWN"))

        repeat(500) { aktorProducer.sendAktorToTopic(aktor, false) }
        kafkaProducerForTest.flush()
        aktorConsumer.ventPaRecords(antall = 500)
        aktorConsumer.isConsumerReady() `should be` true

        // Er klar når siste offset er lest
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/health/kafka"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }
}
