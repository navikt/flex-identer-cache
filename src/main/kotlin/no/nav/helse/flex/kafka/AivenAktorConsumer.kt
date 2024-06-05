package no.nav.helse.flex.kafka
import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.AktorService
import no.nav.helse.flex.util.Metrikk
import no.nav.helse.flex.util.toAktor
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class AivenAktorConsumer(
    private val metrikk: Metrikk,
    private val aktorService: AktorService,
) {
    val log = logger()

    @KafkaListener(
        topics = [AKTOR_TOPIC],
        id = "identer",
        idIsGroup = true,
        containerFactory = "kafkaAvroListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(
        cr: ConsumerRecord<String, GenericRecord>,
        acknowledgment: Acknowledgment,
    ) {
        prosesserPersonhendelse(
            // cr.value(),
            // cr.timestamp(),
            cr,
        )

        acknowledgment.acknowledge()
    }

    fun prosesserPersonhendelse(personhendelse: ConsumerRecord<String, GenericRecord>) {
        metrikk.personHendelseMottatt()
        log.info("motatt")

        handleAktor(personhendelse)
    }

    private fun handleAktor(it: ConsumerRecord<String, GenericRecord>) { // removed suspend here
        val aktorId = it.key()
        val aktor = it.value().toAktor(aktorId)
        aktorService.lagreAktor(aktor)
    }
}
