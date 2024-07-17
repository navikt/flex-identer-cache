package no.nav.helse.flex.kafka
import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.AktorService
import no.nav.helse.flex.util.Metrikk
import no.nav.helse.flex.util.osloZone
import no.nav.helse.flex.util.serialisertTilString
import no.nav.helse.flex.util.toIdentListe
import org.apache.avro.generic.GenericRecord
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
    fun listen(
        consumerRecord: ConsumerRecord<String, GenericRecord>,
        acknowledgment: Acknowledgment,
    ) {
        metrikk.personHendelseMottatt()
        log.info("Mottok kafka melding: $consumerRecord")
        log.info("identer ${consumerRecord.value().toIdentListe().serialisertTilString()}")

        try {
            val aktorId = consumerRecord.key()
            val identifikatorer = consumerRecord.value().toIdentListe()
            identifikatorer.forEach { identifikator -> identifikator.oppdatert = OffsetDateTime.now(osloZone) }
            val aktor = Aktor(aktorId).also { it.identifikatorer = identifikatorer }
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
