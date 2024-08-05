package no.nav.helse.flex.kafka

import no.nav.helse.flex.repository.Aktor
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component

@Component
class AktorProducer(
    private val kafkaProducer: KafkaProducer<String, ByteArray>,
) {
    fun sendAktorToTopic(
        topic: String,
        aktor: Aktor,
    ) {
        sendAktor(AKTOR_TOPIC, aktor)
    }

    fun sendAktor(
        topic: String,
        aktor: Aktor,
    ) {
        kafkaProducer.send(ProducerRecord(topic, aktor.aktorId, aktor.serialiser()))
    }
}
