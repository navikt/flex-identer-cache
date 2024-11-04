package no.nav.helse.flex

import no.nav.helse.flex.kafka.AktorConsumer
import no.nav.helse.flex.kafka.AktorProducer
import no.nav.helse.flex.kafka.KafkaConfig
import no.nav.helse.flex.model.AktorService
import no.nav.helse.flex.testoppsett.startAlleContainere
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.web.servlet.MockMvc
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class, KafkaConfig::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
abstract class FellesTestOppsett {
    @Autowired
    lateinit var server: MockOAuth2Server

    @Autowired
    lateinit var mockMvc: MockMvc

//    @Autowired
//    lateinit var aktorRepository: AktorRepository

    @Autowired
    lateinit var aktorService: AktorService

    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    lateinit var kafkaProducerForTest: KafkaProducer<String, GenericRecord>

    @Autowired
    lateinit var aktorProducer: AktorProducer

//    @AfterAll
//    fun `Vi resetter databasen`() {
//        aktorRepository.deleteAll()
//    }

    @Autowired
    lateinit var aktorConsumer: AktorConsumer

    companion object {
        init {
            startAlleContainere()
        }
    }

    fun tokenxToken(
        fnr: String = "12345678910",
        audience: String = "flex-identer-cache-client-id",
        issuerId: String = "tokenx",
        clientId: String = "dev-gcp:flex:spinnsyn-frontend",
        claims: Map<String, Any> =
            mapOf(
                "acr" to "idporten-loa-high",
                "idp" to "idporten",
                "client_id" to clientId,
                "pid" to fnr,
            ),
    ): String {
        return server.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = UUID.randomUUID().toString(),
                audience = listOf(audience),
                claims = claims,
                expiry = 3600,
            ),
        ).serialize()
    }
}

fun FellesTestOppsett.buildAzureClaimSet(
    clientId: String,
    issuer: String = "azureator",
    azpName: String,
    audience: String = "flex-identer-cache-client-id",
): String {
    val claims = HashMap<String, String>()
    claims.put("azp_name", azpName)
    return server.token(
        subject = "whatever",
        issuerId = issuer,
        clientId = clientId,
        audience = audience,
        claims = claims,
    )
}

fun MockOAuth2Server.token(
    subject: String,
    issuerId: String,
    clientId: String = UUID.randomUUID().toString(),
    audience: String,
    claims: Map<String, Any> = mapOf("acr" to "idporten-loa-high"),
): String {
    return this.issueToken(
        issuerId,
        clientId,
        DefaultOAuth2TokenCallback(
            issuerId = issuerId,
            subject = subject,
            audience = listOf(audience),
            claims = claims,
            expiry = 3600,
        ),
    ).serialize()
}
