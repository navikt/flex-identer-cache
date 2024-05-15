package no.nav.helse.flex.repository

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktorService(private val aktorRepository: AktorRepository) {

    @Transactional
    fun lagreAktor(aktor: Aktor) {
        aktorRepository.lagreAktor(aktor.aktorId)
        aktor.identifikatorer.forEach { identifikator ->
            aktorRepository.lagreIdentifikator(
                idnummer = identifikator.idnummer,
                oppdatert = identifikator.oppdatert,
                type = identifikator.type,
                gjeldende = identifikator.gjeldende,
                aktorId = aktor.aktorId
            )
        }
    }
}
