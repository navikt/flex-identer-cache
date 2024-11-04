import no.nav.helse.flex.kafka.AktorConsumer
import no.nav.helse.flex.logger
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class KafkaHealthIndicator(
    private val aktorConsumer: AktorConsumer,
) : HealthIndicator {
    val log = logger()

    override fun health(): Health {
        return if (aktorConsumer.isConsumerReady()) {
            log.info("Aktor consumer klar")
            Health.up().withDetail("Kafka", "Kafka prosessering er gjennomført").build()
        } else {
            log.info("Aktor consumer ikke klar")
            Health.down().withDetail("Kafka", "Kafka har ikke prosessert alle records enda").build()
        }
    }
}
