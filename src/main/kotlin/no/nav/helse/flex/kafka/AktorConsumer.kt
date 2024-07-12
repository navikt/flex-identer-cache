package no.nav.helse.flex.kafka
import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.AktorService
import no.nav.helse.flex.util.Metrikk
import no.nav.helse.flex.util.serialisertTilString
import no.nav.helse.flex.util.toAktor
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.BinaryDecoder
import org.apache.avro.io.DecoderFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue

@Component
class AktorConsumer(
    private val metrikk: Metrikk,
    private val aktorService: AktorService,
) {
    val log = logger()

    private val schema: Schema =
        Schema.Parser().parse(
            this::class.java.classLoader.getResourceAsStream("avro/aktor.avsc"),
        )

    val buffer = ArrayBlockingQueue<Aktor>(1000)

    @KafkaListener(
        topics = [AKTOR_TOPIC],
        // TODO endre ved prodsetting
        id = "flex-aktor-dev-v2",
        idIsGroup = true,
        containerFactory = "kafkaAvroListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, ByteArray>,
        acknowledgment: Acknowledgment,
    ) {
        metrikk.personHendelseMottatt()
        log.info("mottok aktør med id ${consumerRecord.key()}")

        val message = consumerRecord.value()
        if (message == null) {
            log.warn("Fikk tom melding. Hopper over prossessering")
            acknowledgment.acknowledge()
            return
        }

        try {
            val datumReader = GenericDatumReader<GenericRecord>(schema)
            val decoder: BinaryDecoder = DecoderFactory.get().binaryDecoder(message, null)
            val record = datumReader.read(null, decoder)

            val aktorId = consumerRecord.key()
            val aktor = record.toAktor(aktorId)
            log.info("Forsøker å lagre aktør: ${aktor.serialisertTilString()}")
            buffer.offer(aktor)
            aktorService.lagreAktor(aktor)
        } catch (e: Exception) {
            log.error("Prossessering av melding feilet: ${e.message}. ${e.stackTrace}")
        } finally {
            acknowledgment.acknowledge()
        }
    }
}
