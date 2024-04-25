import no.nav.helse.flex.kafka.PdlPersonService
import no.nav.helse.flex.model.Ident
import no.nav.helse.flex.model.IdentType
import no.nav.syfo.pdl.error.InactiveIdentException
import java.time.LocalDate
//import no.nav.syfo.sykmelding.db.Periode
//import no.nav.syfo.sykmelding.db.SykmeldingDbModelUtenBehandlingsutfall
//import no.nav.syfo.sykmelding.db.getSykmeldingerMedIdUtenBehandlingsutfallForFnr
//import no.nav.syfo.sykmelding.db.updateFnr
//import no.nav.syfo.sykmelding.kafka.model.ArbeidsgiverStatusKafkaDTO
//import no.nav.syfo.sykmelding.kafka.model.KafkaMetadataDTO
//import no.nav.syfo.sykmelding.kafka.model.STATUS_SENDT
//import no.nav.syfo.sykmelding.kafka.model.ShortNameKafkaDTO
//import no.nav.syfo.sykmelding.kafka.model.SporsmalOgSvarKafkaDTO
//import no.nav.syfo.sykmelding.kafka.model.SvartypeKafkaDTO
//import no.nav.syfo.sykmelding.kafka.model.SykmeldingKafkaMessage
//import no.nav.syfo.sykmelding.kafka.model.SykmeldingStatusKafkaEventDTO
//import no.nav.syfo.sykmelding.kafka.model.toArbeidsgiverSykmelding
//import no.nav.syfo.sykmelding.kafka.producer.SendtSykmeldingKafkaProducer
//import no.nav.syfo.sykmelding.status.getAlleSpm

class IdentendringService(
    private val database: DatabaseInterface,
    private val sendtSykmeldingKafkaProducer: SendtSykmeldingKafkaProducer,
    private val pdlService: PdlPersonService,
) {
    suspend fun oppdaterIdent(identListe: List<Ident>): Int {
        if (harEndretFnr(identListe)) {
            val nyttFnr =
                identListe
                    .find { it.type == IdentType.FOLKEREGISTERIDENT && it.gjeldende }
                    ?.idnummer
                    ?: throw IllegalStateException("Mangler gyldig fnr!")

            val tidligereFnr =
                identListe.filter { it.type == IdentType.FOLKEREGISTERIDENT && !it.gjeldende }

            }
        return 0
    }

    private suspend fun sjekkPDL(nyttFnr: String) {
        val pdlPerson = pdlService.getPdlPerson(nyttFnr)
        if (
            pdlPerson.fnr != nyttFnr ||
            pdlPerson.identer.any { it.ident == nyttFnr && it.historisk }
        ) {
            throw InactiveIdentException("Nytt FNR er ikke aktivt FNR i PDL API")
        }
    }

    private fun harEndretFnr(identListe: List<Ident>): Boolean {
        if (identListe.filter { it.type == IdentType.FOLKEREGISTERIDENT }.size < 2) {
            log.debug("Identendring inneholder ingen endring i fnr")
            return false
        }
        return true
    }


    private fun finnSisteTom(perioder: List<Periode>): LocalDate {
        return perioder.maxByOrNull { it.tom }?.tom
            ?: throw IllegalStateException("Skal ikke kunne ha periode uten tom")
    }
}
