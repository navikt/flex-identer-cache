package no.nav.helse.flex.kafka
import no.nav.helse.flex.logger
import no.nav.helse.flex.util.Metrikk
import no.nav.helse.flex.util.tilOsloZone
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AivenIdenterConsumer(
    private val metrikk: Metrikk,
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
            cr.value(),
            cr.timestamp(),
        )

        acknowledgment.acknowledge()
    }

    fun prosesserPersonhendelse(
        personhendelse: GenericRecord,
        timestamp: Long,
    ) {
        metrikk.personHendelseMottatt()

        if (personhendelse.erDodsfall) {
            metrikk.dodsfallMottatt()

            val identer = identService.hentFolkeregisterIdenterMedHistorikkForFnr(personhendelse.fnr)

            if (harUutfylteSoknader(identer)) {
                when (personhendelse.endringstype) {
                    OPPRETTET, KORRIGERT -> {
                        val dodsdato = personhendelse.dodsdato

                        if (dodsmeldingDAO.harDodsmelding(identer)) {
                            log.info("Oppdaterer dodsdato")
                            dodsmeldingDAO.oppdaterDodsdato(identer, dodsdato)
                        } else {
                            log.info("Lagrer ny dodsmelding")
                            dodsmeldingDAO.lagreDodsmelding(identer, dodsdato, Instant.ofEpochMilli(timestamp).tilOsloZone())
                        }
                    }
                    ANNULLERT, OPPHOERT -> {
                        log.info("Sletter dodsmelding")
                        dodsmeldingDAO.slettDodsmelding(identer)
                    }
                }
            }
        } else {
            log.debug("Ignorerer personhendelse med type ${personhendelse.opplysningstype}")
        }
    }

    private fun harUutfylteSoknader(identer: FolkeregisterIdenter) =
        sykepengesoknadDAO.finnSykepengesoknader(identer)
            .any { listOf(Soknadstatus.NY, Soknadstatus.FREMTIDIG).contains(it.status) }
}
