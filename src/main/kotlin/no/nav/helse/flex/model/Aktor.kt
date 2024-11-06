package no.nav.helse.flex.model

import org.springframework.data.annotation.Id
import java.time.OffsetDateTime

data class Aktor(
    @Id
    var aktorId: String? = null,
    var identifikatorer: List<Identifikator> = mutableListOf(),
) {
    init {
        this.verifiser()
    }

    private fun verifiser() {
        requireNotNull(this.aktorId) { "Aktor ID kan ikke være null" }
        val identifikatorer = requireNotNull(this.identifikatorer) { "Identifikatorer kan ikke være null" }

        identifikatorer.forEach { identifikator ->
            requireNotNull(identifikator.idnummer) { "ID-nummer kan ikke være null" }
            requireNotNull(identifikator.oppdatert) { "Oppdatert kan ikke være null" }
            requireNotNull(identifikator.type?.name) { "Type kan ikke være null" }
            requireNotNull(identifikator.gjeldende) { "Gjeldende kan ikke være null" }
        }
    }

    companion object {
        fun sanitizeKey(key: String): String {
            // strips off all non-ASCII characters
            var text = key
            text = text.replace("[^\\x00-\\x7F]".toRegex(), "")

            // erases all the ASCII control characters
            text = text.replace("\\p{Cntrl}&&[^\r\n\t]".toRegex(), "")

            // removes non-printable characters from Unicode
            text = text.replace("\\p{C}".toRegex(), "")
            return text.trim().filter { it.isDigit() }
        }
    }
}

data class Identifikator(
    @Id
    var idnummer: String? = null,
    var type: Type? = null,
    var gjeldende: Boolean? = null,
    var oppdatert: OffsetDateTime? = null,
)

enum class Type {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}
