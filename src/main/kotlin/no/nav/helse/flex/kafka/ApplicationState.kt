package no.nav.helse.flex.kafka

data class ApplicationState(
    var alive: Boolean = true,
    var ready: Boolean = true,
)
