package no.nav.helse.flex.model

import org.springframework.data.annotation.Id
import java.time.OffsetDateTime

data class Aktor(
    @Id
    var aktorId: String,
    var identifikatorer: List<Identifikator>,
) {
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
    var idnummer: String,
    var type: Type,
    var gjeldende: Boolean,
    var oppdatert: OffsetDateTime,
)

enum class Type {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}
