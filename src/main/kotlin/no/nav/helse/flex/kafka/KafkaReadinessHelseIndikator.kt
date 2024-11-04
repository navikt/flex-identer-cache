import no.nav.helse.flex.kafka.AktorConsumer
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class KafkaReadinessHelseIndikator(
    private val aktorConsumer: AktorConsumer,
) : HealthIndicator {
    override fun health(): Health {
        return if (aktorConsumer.isConsumerReady()) {
            Health.up().withDetail("kafka", "Kafka processing is complete and ready").build()
        } else {
            Health.down().withDetail("kafka", "Kafka processing is not yet complete").build()
        }
    }
}
