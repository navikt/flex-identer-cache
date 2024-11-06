package no.nav.helse.flex

import no.nav.helse.flex.model.Aktor
import no.nav.helse.flex.model.Identifikator
import no.nav.helse.flex.model.Type
import no.nav.helse.flex.util.tilOsloZone
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.OffsetDateTime

class FlexIdenterCacheApiTest: FellesTestOppsett() {
    @Test
    fun hentIdenterForAktorTest(){
        val identifikator = Identifikator(
            idnummer = "234567",
            type = Type.FOLKEREGISTERIDENT,
            gjeldende = true,
            oppdatert = OffsetDateTime.now().tilOsloZone()
        )
        val aktor = Aktor("aktorId", listOf(identifikator))
        aktorService.lagreFlereAktorer(listOf(aktor))

        mockMvc.perform(
            get("/api/v1/identer/${aktor.aktorId}")
                .header("Authorization", "Bearer ${skapAzureJwt("sykepengesoknad-backend-client-id")}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString
    }
}
