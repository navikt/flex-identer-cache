package no.nav.helse.flex.kafka

import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.logger
import no.nav.helse.flex.model.Aktor
import no.nav.helse.flex.model.AktorService
import no.nav.helse.flex.util.Metrikk
import no.nav.helse.flex.util.toAktor
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

@Component
class AktorConsumer(
    private val metrikk: Metrikk,
    private val aktorService: AktorService,
    private val environmentToggles: EnvironmentToggles,
    val buffer: ArrayBlockingQueue<Aktor>? = null,
) {
    val log = logger()
    private val isReady: AtomicBoolean = AtomicBoolean(false)

    @KafkaListener(
        topics = [AKTOR_TOPIC],
        id = "flex-identer-cache",
        idIsGroup = false,
        containerFactory = "kafkaAvroListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(
        consumerRecords: ConsumerRecords<String, GenericRecord>,
        consumer: Consumer<String, GenericRecord>,
    ) {
        metrikk.personHendelseMottatt()
        log.info("Mottok ${consumerRecords.count()} aktør records")

        var totalByteSize = 0
        val time =
            measureTimeMillis {
                val aktorList =
                    consumerRecords.mapNotNull { consumerRecord: ConsumerRecord<String, GenericRecord> ->
                        try {
                            totalByteSize += consumerRecord.serializedValueSize()
                            val aktorId = Aktor.sanitizeKey(consumerRecord.key())
                            return@mapNotNull consumerRecord.value()?.toAktor(aktorId)
                        } catch (e: Exception) {
                            if (environmentToggles.isQ()) {
                                log.error("Klarte ikke prosessere record med key: ${consumerRecord.key()}: ${e.message}", e)
                            }
                            return@mapNotNull null
                        }
                    }
                aktorService.lagreFlereAktorer(aktorList)
                isReady.set(harLestHeleTopicet(consumer).also { log.info("lest hele: $it") })
                aktorList.forEach { aktor -> buffer?.offer(aktor) }
            }
        log.info("Prossesserte ${consumerRecords.count()} records, med størrelse $totalByteSize bytes, iløpet av $time millisekunder")
    }

    fun harLestHeleTopicet(kafkaConsumer: Consumer<String, GenericRecord>): Boolean {
        val partitions = kafkaConsumer.assignment()
        if (partitions.isEmpty()) return false

        val endOffsets = kafkaConsumer.endOffsets(partitions)

        for (partition in partitions) {
            val denneOffset = kafkaConsumer.position(partition)
            val sisteOffset = endOffsets[partition] ?: continue
            if (denneOffset < sisteOffset) {
                return false
            }
        }
        log.info("Lest hele topicet")
        return true
    }

    fun isConsumerReady(): Boolean = isReady.get()
}
