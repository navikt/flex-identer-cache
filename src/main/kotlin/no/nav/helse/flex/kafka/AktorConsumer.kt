package no.nav.helse.flex.kafka

import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.AktorService
import no.nav.helse.flex.util.Metrikk
import no.nav.helse.flex.util.osloZone
import no.nav.helse.flex.util.serialisertTilString
import no.nav.helse.flex.util.toAktor
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.annotation.KafkaListener
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
        id = "flex-aktor-dev-v14",
        idIsGroup = true,
        containerFactory = "kafkaAvroListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(consumerRecords: ConsumerRecords<String, GenericRecord>) {
        metrikk.personHendelseMottatt()
        log.info("Mottok ${consumerRecords.count()} aktør hendelser")

        try {
            consumerRecords.map { consumerRecord: ConsumerRecord<String, GenericRecord> ->
                val aktorId = Aktor.sanitizeKey(consumerRecord.key())
                val aktor = consumerRecord.value().toAktor(aktorId)
                aktor.also {
                    it.identifikatorer.forEach { identifikator -> identifikator.oppdatert = OffsetDateTime.now(osloZone) }
                }
                log.info("Forsøker å lagre aktør: ${aktor.serialisertTilString()}")
                buffer.offer(aktor)
                aktorService.lagreAktor(aktor)
                log.info("Aktor $aktorId ble lagret")
            }
        } catch (e: Exception) {
            log.error(e.message)
        } finally {
            log.info("Håndterte alle aktører hendelser")
        }
    }
}
