package no.nav.helse.flex

import no.nav.helse.flex.kafka.AktorConsumer
import no.nav.helse.flex.kafka.AktorProducer
import no.nav.helse.flex.model.AktorService
import no.nav.helse.flex.testoppsett.startAlleContainere
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
abstract class FellesTestOppsett {
    @Autowired
    lateinit var server: MockOAuth2Server

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var aktorService: AktorService

    @Autowired
    lateinit var kafkaProducerForTest: KafkaProducer<String, GenericRecord>

    @Autowired
    lateinit var aktorProducer: AktorProducer

    @Autowired
    lateinit var aktorConsumer: AktorConsumer

    companion object {
        init {
            startAlleContainere()
        }
    }
}
