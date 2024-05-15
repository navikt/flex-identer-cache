package no.nav.helse.flex.kafka.producer

import no.nav.helse.flex.EnvironmentToggles
import no.nav.helse.flex.kafka.AKTOR_TOPIC
import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.Aktor
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AivenKafkaProducer(
    @Autowired
    private val producer: KafkaProducer<String, GenericRecord>,
    private val environmentToggles: EnvironmentToggles,
) {
    val log = logger()

    // TODO Brukes kanskje kun til testing?
    fun produserMelding(aktor: Aktor): RecordMetadata {
        try {
            if (environmentToggles.isQ()) {
                log.info("Publiserer identer på topic $AKTOR_TOPIC\n")
            }
            return producer.send(
                ProducerRecord(
                    AKTOR_TOPIC,
                    aktor.aktorId,
                    aktor.tilGenericRecord(),
                ),
            ).get()
        } catch (e: Throwable) {
            log.error("Uventet exception ved publisering av aktor på topic $AKTOR_TOPIC", e)
            // get() kaster InterruptedException eller ExecutionException. Begge er checked, så pakker  de den inn i
            // en RuntimeException da en CheckedException kan forhindre rollback i metoder annotert med @Transactional.
            throw AivenKafkaException(e)
        }
    }
}

class AivenKafkaException(e: Throwable) : RuntimeException(e)
