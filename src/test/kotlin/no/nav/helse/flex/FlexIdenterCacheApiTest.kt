package no.nav.helse.flex

import no.nav.helse.flex.model.Aktor
import no.nav.helse.flex.model.Identifikator
import no.nav.helse.flex.model.Type
import no.nav.helse.flex.util.OBJECT_MAPPER
import no.nav.helse.flex.util.tilOsloZone
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be`
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.OffsetDateTime

class FlexIdenterCacheApiTest : FellesTestOppsett() {
    @Test
    fun hentIdenterForAktorTest() {
        val identifikator =
            Identifikator(
                idnummer = "234567",
                type = Type.FOLKEREGISTERIDENT,
                gjeldende = true,
                oppdatert = OffsetDateTime.now().tilOsloZone(),
            )
        val identifikator2 =
            Identifikator(
                idnummer = "345665",
                type = Type.NPID,
                gjeldende = false,
                oppdatert = OffsetDateTime.now().tilOsloZone(),
            )
        val identifikator3 =
            Identifikator(
                idnummer = "734745",
                type = Type.AKTORID,
                gjeldende = false,
                oppdatert = OffsetDateTime.now().tilOsloZone(),
            )
        val aktor = Aktor(identifikator3.idnummer, listOf(identifikator, identifikator2, identifikator3))
        aktorService.lagreFlereAktorer(listOf(aktor))

        val json =
            mockMvc.perform(
                post("/api/v1/identer/aktor")
                    .header("Authorization", "Bearer ${skapAzureJwt("sykepengesoknad-backend-client-id")}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .queryParam("aktorId", aktor.aktorId),
            ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

        json `should not be` null
        val hentetAktor = OBJECT_MAPPER.readValue(json, Aktor::class.java)
        hentetAktor.aktorId `should be equal to` aktor.aktorId
    }

    @Test
    fun hentIdenterForIdentTest() {
        val identifikator =
            Identifikator(
                idnummer = "234567",
                type = Type.FOLKEREGISTERIDENT,
                gjeldende = true,
                oppdatert = OffsetDateTime.now().tilOsloZone(),
            )
        val identifikator2 =
            Identifikator(
                idnummer = "345665",
                type = Type.NPID,
                gjeldende = false,
                oppdatert = OffsetDateTime.now().tilOsloZone(),
            )
        val identifikator3 =
            Identifikator(
                idnummer = "734745",
                type = Type.AKTORID,
                gjeldende = false,
                oppdatert = OffsetDateTime.now().tilOsloZone(),
            )
        val aktor = Aktor(identifikator3.idnummer, listOf(identifikator, identifikator2, identifikator3))
        aktorService.lagreFlereAktorer(listOf(aktor))

        val json =
            mockMvc.perform(
                post("/api/v1/identer/ident")
                    .header("Authorization", "Bearer ${skapAzureJwt("sykepengesoknad-backend-client-id")}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .queryParam("ident", identifikator.idnummer),
            ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

        json `should not be` null
        val hentetAktor = OBJECT_MAPPER.readValue(json, Aktor::class.java)
        hentetAktor.aktorId `should be equal to` aktor.aktorId
    }
}
