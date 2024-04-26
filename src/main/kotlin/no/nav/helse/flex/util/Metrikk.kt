package no.nav.helse.flex.util

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Component

@Component
class Metrikk(private val registry: MeterRegistry) {
    fun dodsfallMottatt() {
        registry.counter("syfosoknad_dodsmelding_mottatt", Tags.of("type", "info")).increment()
    }

    fun personHendelseMottatt() {
        registry.counter("syfosoknad_personhendelse_mottatt", Tags.of("type", "info")).increment()
    }
}
