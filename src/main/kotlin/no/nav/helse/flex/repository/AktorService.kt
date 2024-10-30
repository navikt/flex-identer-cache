package no.nav.helse.flex.repository

import no.nav.helse.flex.logger
import no.nav.helse.flex.util.OBJECT_MAPPER
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktorService() {
    val aktorHashMap = HashMap<String, Aktor>()
    val log = logger()

    fun verifiserAktor(aktor: Aktor): Boolean {
        try {
            requireNotNull(aktor.aktorId) { "Aktor ID kan ikke være null" }
            val identifikatorer = requireNotNull(aktor.identifikatorer) { "Identifikatorer kan ikke være null" }

            identifikatorer.forEach { identifikator ->
                requireNotNull(identifikator.idnummer) { "ID-nummer kan ikke være null" }
                requireNotNull(identifikator.oppdatert) { "Oppdatert kan ikke være null" }
                requireNotNull(identifikator.type?.name) { "Type kan ikke være null" }
                requireNotNull(identifikator.gjeldende) { "Gjeldende kan ikke være null" }
            }
            return true
        } catch (e: Exception) {
            log.error("Feil på aktør med aktørId ${aktor.aktorId}", e.message)
            return false
        }
    }

    @Transactional
    fun lagreFlereAktorer(aktorList: List<Aktor>) {
        aktorList.forEach { aktor ->
            verifiserAktor(aktor)
            aktorHashMap.put(aktor.aktorId!!, aktor)
        }
    }

    fun hentAktor(aktorId: String): Aktor? {
        return aktorHashMap[aktorId]
    }
}
