package no.nav.helse.flex.kafka

import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.AktorService
import no.nav.helse.flex.util.Metrikk
import no.nav.helse.flex.util.toAktor
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue
import kotlin.system.measureTimeMillis

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
        id = "flex-identer-cache-v9",
        idIsGroup = true,
        containerFactory = "kafkaAvroListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(consumerRecords: ConsumerRecords<String, GenericRecord>) {
        metrikk.personHendelseMottatt()
        log.info("Mottok ${consumerRecords.count()} aktør hendelser")

        try {
            var totalByteSize = 0
            val time =
                measureTimeMillis {
                    val aktorList =
                        consumerRecords.map { consumerRecord: ConsumerRecord<String, GenericRecord> ->
                            totalByteSize += consumerRecord.serializedValueSize()
                            val aktorId = Aktor.sanitizeKey(consumerRecord.key())
                            val aktor = consumerRecord.value().toAktor(aktorId)
                            return@map aktor
                        }
                    aktorService.lagreFlereAktorer(aktorList)
                    aktorList.forEach { aktor -> buffer.offer(aktor) }
                }
            log.info("Prossesserte ${consumerRecords.count()} records, med størrelse $totalByteSize bytes, iløpet av $time millis")
        } catch (e: Exception) {
            log.error(e.message)
        } finally {
            log.info("Håndterte alle aktører hendelser")
        }
    }
}
