package no.nav.helse.flex

import no.nav.helse.flex.kafka.AivenAktorConsumer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.awaitility.Awaitility.await
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

fun <K, V> Consumer<K, V>.subscribeHvisIkkeSubscribed(vararg topics: String) {
    if (this.subscription().isEmpty()) {
        this.subscribe(listOf(*topics))
    }
}

fun <K, V> Consumer<K, V>.hentProduserteRecords(duration: Duration = Duration.ofMillis(100)): List<ConsumerRecord<K, V>> {
    return this.poll(duration).also {
        this.commitSync()
    }.iterator().asSequence().toList()
}

fun <K, V> Consumer<K, V>.ventPåRecords(
    antall: Int,
    duration: Duration = Duration.ofSeconds(9),
): List<ConsumerRecord<K, V>> {
    val factory =
        if (antall == 0) {
            // Må vente fullt ut, ikke opp til en tid siden vi vil se at ingen blir produsert
            await().during(duration)
        } else {
            await().atMost(duration)
        }

    val alle = ArrayList<ConsumerRecord<K, V>>()
    runCatching {
        factory.until {
            alle.addAll(this.hentProduserteRecords())
            alle.size == antall
        }
    }.onFailure {
        println("Forventet $antall meldinger på kafka, mottok ${alle.size}")
        throw it
    }

    return alle
}

fun AivenAktorConsumer.ventPåRecords(
    antall: Int,
    duration: Duration = Duration.ofSeconds(9),
): List<ConsumerRecord<String, GenericRecord>> {
    val alle = ArrayList<ConsumerRecord<String, GenericRecord>>()
    val deadline = System.currentTimeMillis() + duration.toMillis()
    val buffer = ArrayBlockingQueue<ConsumerRecord<String, GenericRecord>>(1000)

    while (System.currentTimeMillis() < deadline && alle.size < antall) {
        val remainingTime = deadline - System.currentTimeMillis()
        val record = buffer.poll(remainingTime, TimeUnit.MILLISECONDS)
        record?.let { alle.add(it) }
    }

    if (alle.size != antall) {
        log.error("Forventet $antall meldinger på kafka, mottok ${alle.size}")
        throw RuntimeException("Expected $antall messages on Kafka, received ${alle.size}")
    }

    return alle
}
