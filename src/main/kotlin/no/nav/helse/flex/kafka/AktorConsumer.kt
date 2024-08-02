package no.nav.helse.flex.kafka
import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.AktorService
import no.nav.helse.flex.repository.deserialiserTilAktor
import no.nav.helse.flex.util.Metrikk
import no.nav.helse.flex.util.osloZone
import no.nav.helse.flex.util.serialisertTilString
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.concurrent.ArrayBlockingQueue

@Component
class AktorConsumer(
    private val metrikk: Metrikk,
    private val aktorService: AktorService,
) {
    val log = logger()
    val buffer = ArrayBlockingQueue<Aktor>(1000)

    @KafkaListener(
        topics = [AKTOR_TOPIC],
        // TODO endre ved prodsetting
        id = "flex-aktor-dev-v13",
        idIsGroup = true,
        containerFactory = "kafkaAvroListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listenByteArray(
        consumerRecord: ConsumerRecord<String, ByteArray>,
        acknowledgment: Acknowledgment,
    ) {
        metrikk.personHendelseMottatt()
        log.info("Mottok kafka melding: $consumerRecord")

        try {
            val aktor =
                consumerRecord.value().deserialiserTilAktor()
                    .also { it.identifikatorer.forEach { identifikator -> identifikator.oppdatert = OffsetDateTime.now(osloZone) } }
            log.info("Forsøker å lagre aktør: ${aktor.serialisertTilString()}")
            buffer.offer(aktor)
            aktorService.lagreAktor(aktor)
        } catch (e: Exception) {
            log.error("Prossessering av melding feilet: ${e.message}. ${e.stackTraceToString()}")
        } finally {
            acknowledgment.acknowledge()
        }
    }
}
