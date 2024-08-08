package no.nav.helse.flex.util

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Component

@Component
class Metrikk(private val registry: MeterRegistry) {
    fun personHendelseMottatt() {
        registry.counter("flex_identer_cache_personhendelse_mottatt", Tags.of("type", "info")).increment()
    }
}
