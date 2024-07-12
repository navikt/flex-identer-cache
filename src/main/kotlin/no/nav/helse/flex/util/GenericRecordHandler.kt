package no.nav.helse.flex.util

import no.nav.helse.flex.repository.Aktor
import no.nav.helse.flex.repository.IdentType
import no.nav.helse.flex.repository.Identifikator
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import java.time.OffsetDateTime

fun GenericRecord.toIdentListe(): List<Identifikator> {
    val data = this.get("identifikatorer")

    if (data !is GenericData.Array<*>) {
        throw IllegalArgumentException("Feil data type for 'identifikatorer'")
    }

    return data.filterIsInstance<GenericRecord>().map { identifikator ->
        val type =
            when (val typeString = identifikator.get("type").toString()) {
                "FOLKEREGISTERIDENT" -> IdentType.FOLKEREGISTERIDENT
                "AKTORID" -> IdentType.AKTORID
                "NPID" -> IdentType.NPID
                else -> throw IllegalStateException("Mottok ident med ukjent type: $typeString")
            }
        Identifikator(
            idnummer = identifikator.get("idnummer").toString(),
            gjeldende = identifikator.get("gjeldende").toString().toBoolean(),
            type = type.name,
            oppdatert = OffsetDateTime.now(),
        )
    }
}

fun GenericRecord.toAktor(aktorId: String): Aktor {
    val identifikatorer = this["identifikatorer"]
    if (identifikatorer !is List<*>) {
        throw IllegalStateException("Expected a list of identifikatorer, but got: $identifikatorer")
    }

    val parsetIdentifikatorer =
        identifikatorer.map { identifikator ->
            if (identifikator is GenericRecord) {
                val typeString = identifikator["type"].toString()
                val type =
                    try {
                        IdentType.valueOf(typeString)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalStateException("Mottok ident med ukjent type: $typeString")
                    }
                Identifikator(
                    idnummer = identifikator["idnummer"].toString(),
                    type = type.name,
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
