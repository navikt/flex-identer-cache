package no.nav.helse.flex.util

import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.Identifikator
import no.nav.helse.flex.repository.Type
import org.apache.avro.generic.GenericArray
import org.apache.avro.generic.GenericRecord
import java.time.OffsetDateTime

fun GenericRecord.toAktor(aktorId: String): Aktor {
    val identifikatorer = (get("identifikatorer") as GenericArray<*>)

    val parsetIdentifikatorer =
        identifikatorer.map { identifikator ->
            if (identifikator is GenericRecord) {
                val typeString = identifikator["type"].toString()
                val type =
                    try {
                        Type.valueOf(typeString)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalStateException("Mottok ident med ukjent type: $typeString")
                    }
                Identifikator(
                    idnummer = identifikator["idnummer"].toString(),
                    type = type,
                    gjeldende = identifikator["gjeldende"].toString().toBoolean(),
                    oppdatert = OffsetDateTime.now(),
                )
            } else {
                throw IllegalStateException("Feil data type for 'identifikatorer': $identifikatorer")
            }
        }

    return Aktor(
        aktorId = aktorId,
        identifikatorer = parsetIdentifikatorer,
    )
}
