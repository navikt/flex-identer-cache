package no.nav.helse.flex

import org.junit.jupiter.api.AfterEach

class IntegrationTest : FellesTestOppsett() {
    @AfterEach
    fun slettFraDatabase() {
        identerRepository.deleteAll()
    }
}
