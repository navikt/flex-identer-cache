package no.nav.helse.flex.kafka

import no.nav.helse.flex.repository.Aktor
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

fun KafkaProducer<String, ByteArray>.sendAktor(
    topic: String,
    aktor: Aktor,
) {
    val json = aktor.serialiser()
    val record = ProducerRecord(topic, aktor.aktorId, json)
    this.send(record)
}
