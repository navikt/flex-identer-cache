package no.nav.helse.flex

import io.getunleash.FakeUnleash
import no.nav.helse.flex.kafka.AivenAktorConsumer
import no.nav.helse.flex.kafka.producer.AivenKafkaProducer
import no.nav.helse.flex.repository.AktorRepository
import no.nav.helse.flex.repository.AktorService
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.concurrent.thread

private class RedisContainer : GenericContainer<RedisContainer>("bitnami/redis:6.2")

private class PostgreSQLContainer14 : PostgreSQLContainer<PostgreSQLContainer14>("postgres:14-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
abstract class FellesTestOppsett {
    @Autowired
    lateinit var server: MockOAuth2Server

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var aktorRepository: AktorRepository

    @Autowired
    lateinit var aktorService: AktorService

    @AfterAll
    fun `Vi resetter databasen`() {
        aktorRepository.deleteAll()
    }

    @Autowired
    lateinit var fakeUnleash: FakeUnleash

    @SpyBean
    @Qualifier("aktorKafkaProducer")
    lateinit var aivenKafkaProducer: AivenKafkaProducer

    @Autowired
    lateinit var aivenAktorConsumer: AivenAktorConsumer

    @AfterAll
    fun `Disable unleash toggles`() {
        fakeUnleash.disableAll()
    }

    companion object {
        init {
            val threads = mutableListOf<Thread>()

            thread {
                RedisContainer().apply {
                    withEnv("ALLOW_EMPTY_PASSWORD", "yes")
                    withExposedPorts(6379)
                    start()

                    System.setProperty("REDIS_URI_SESSIONS", "rediss://$host:$firstMappedPort")
                    System.setProperty("REDIS_USERNAME_SESSIONS", "default")
                    System.setProperty("REDIS_PASSWORD_SESSIONS", "")
                }
            }.also { threads.add(it) }

            thread {
                KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3")).apply {
                    start()
                    System.setProperty("KAFKA_BROKERS", bootstrapServers)
                }
            }.also { threads.add(it) }

            thread {
                PostgreSQLContainer14().apply {
                    // Cloud SQL har wal_level = 'logical' på grunn av flagget cloudsql.logical_decoding i
                    // naiserator.yaml. Vi må sette det samme lokalt for at flyway migrering skal fungere.
                    withCommand("postgres", "-c", "wal_level=logical")
                    start()
                    System.setProperty("spring.datasource.url", "$jdbcUrl&reWriteBatchedInserts=true")
                    System.setProperty("spring.datasource.username", username)
                    System.setProperty("spring.datasource.password", password)
                }
            }.also { threads.add(it) }

            threads.forEach { it.join() }
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
