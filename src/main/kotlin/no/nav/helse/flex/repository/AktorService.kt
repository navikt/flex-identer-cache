package no.nav.helse.flex.repository

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktorService(private val aktorRepository: AktorRepository) {
    @Transactional
    fun lagreAktor(aktor: Aktor) {
        // Validate and ensure all required fields are non-null
        val aktorId = requireNotNull(aktor.aktorId) { "Aktor ID kan ikke være null" }
        val identifikatorer = requireNotNull(aktor.identifikatorer) { "Identifikatorer kan ikke være null" }

        // Save the Aktor
        aktorRepository.lagreAktor(aktorId)

        // Save Identifikatorer
        identifikatorer.forEach { identifikator ->
            val idnummer = requireNotNull(identifikator.idnummer) { "ID-nummer kan ikke være null" }
            val oppdatert = requireNotNull(identifikator.oppdatert) { "Oppdatert kan ikke være null" }
            val typeName = requireNotNull(identifikator.type?.name) { "Type kan ikke være null" }
            val gjeldende = requireNotNull(identifikator.gjeldende) { "Gjeldende kan ikke være null" }

            aktorRepository.lagreIdentifikator(
                idnummer = idnummer,
                oppdatert = oppdatert,
                type = typeName,
                gjeldende = gjeldende,
                aktorId = aktorId,
            )
        }
    }
}
