package no.nav.helse.flex.kafka

import no.nav.helse.flex.logger
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ApiKlarController(private val aktorConsumer: AktorConsumer) {
    val log = logger()

    @Unprotected
    @ResponseBody
    @GetMapping(value = ["/api/klar"], produces = [APPLICATION_JSON_VALUE])
    fun harLestHeleTopic(): ResponseEntity<Void> {
        return if (aktorConsumer.isConsumerReady()) {
            log.info("Lest hele topic")
            ResponseEntity.status(HttpStatus.OK).build()
        } else {
            log.info("Ikke lest topic enda")
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }
    }
}
