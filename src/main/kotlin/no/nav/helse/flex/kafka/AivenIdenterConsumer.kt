package no.nav.helse.flex.kafka
import no.nav.helse.flex.logger
import no.nav.helse.flex.repository.IdenterRepository
import no.nav.helse.flex.util.Metrikk
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.avro.reflect.AvroDoc
import org.apache.avro.reflect.ReflectData
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@AvroDoc("Identer")
data class Ident(
    val idnummer: String,
    val gjeldende: Boolean,
    val type: IdentType,
) {
    fun toGenericRecord(): GenericRecord {
        val avroSchema = ReflectData.get().getSchema(this::class.java)
        return GenericRecordBuilder(avroSchema).apply {
            set("idnummer", this@Ident.idnummer)
            set("gjeldende", this@Ident.gjeldende)
            set("type", this@Ident.type.name)  // Assuming `IdentType` is an enum
        }.build()
    }
}

enum class IdentType {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}

@Component
class AivenIdenterConsumer(
    private val metrikk: Metrikk,
    private val identerRepository: IdenterRepository,
) {
    val log = logger()

    @KafkaListener(
        topics = [IDENTER_TOPIC],
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

        handleIdent(personhendelse)
    }

    private fun handleIdent(it: ConsumerRecord<String, GenericRecord>) { // removed suspend here
        val identListe = it.value().toIdentListe()
        log.info("identliste opprettet" + identListe.size)
        // Lagre ident i repo
        identListe.forEach { ident ->
            log.info("lagrer ident i repo")
            identerRepository.lagre(
                id = ident.idnummer,
                type = ident.type.toString(),
                gjeldende = ident.gjeldende ?: false,
                opprettet = OffsetDateTime.now(),
            )
        }
    }

    fun GenericRecord.toIdentListe(): List<Ident> {
        val data = this.get("identifikatorer")

        if (data !is GenericData.Array<*>) {
            throw IllegalArgumentException("Feil data type for 'identifikatorer'")
        }

        return data.filterIsInstance<GenericRecord>().map {
            val type =
                when (val typeString = it.get("type").toString()) {
                    "FOLKEREGISTERIDENT" -> IdentType.FOLKEREGISTERIDENT
                    "AKTORID" -> IdentType.AKTORID
                    "NPID" -> IdentType.NPID
                    else -> throw IllegalStateException("Mottok ident med ukjent type: $typeString")
                }
            Ident(
                idnummer = it.get("idnummer").toString(),
                gjeldende = it.get("gjeldende").toString().toBoolean(),
                type = type,
            )
        }
    }
}
