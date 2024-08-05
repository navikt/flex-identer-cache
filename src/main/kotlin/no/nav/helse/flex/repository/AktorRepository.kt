package no.nav.helse.flex.repository

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Transactional
@Repository
interface AktorRepository : CrudRepository<Aktor, String> {
    fun findByAktorId(aktorId: String): Aktor?

    @Query("SELECT DISTINCT idnummer FROM identifikator")
    fun finnAlleDistinctIder(): Set<String>

    @Modifying
    @Query(
        """INSERT INTO aktor (aktor_id) 
           VALUES (:aktorId)
           ON CONFLICT (aktor_id) 
           DO NOTHING""",
    )
    fun lagreAktor(
        @Param("aktorId") aktorId: String,
    ): Int

    @Modifying
    @Query(
        """INSERT INTO identifikator (idnummer, oppdatert, type, gjeldende, aktor_id) 
           VALUES (:idnummer, :oppdatert, :type, :gjeldende, :aktorId)
           ON CONFLICT (idnummer) 
           DO NOTHING""",
    )
    fun lagreIdentifikator(
        @Param("idnummer") idnummer: String,
        @Param("oppdatert") oppdatert: OffsetDateTime,
        @Param("type") type: String,
        @Param("gjeldende") gjeldende: Boolean,
        @Param("aktorId") aktorId: String,
    ): Int

    @Query(
        """SELECT a.aktor_id AS aktorId, i.idnummer AS idnummer, i.type AS type, i.gjeldende AS gjeldende 
            FROM aktor a LEFT JOIN identifikator i ON a.aktor_id = i.aktor_id WHERE a.aktor_id = :aktorId""",
    )
    fun finnIdentifikatorerFraAktorId(
        @Param("aktorId") aktorId: String,
    ): Aktor?
}

@Serializable
data class Aktor(
    @Id
    var aktorId: String = "",
    @MappedCollection(idColumn = "aktor_id", keyColumn = "aktor_id")
    var identifikatorer: List<Identifikator> = mutableListOf(),
) {
    fun serialiser(): ByteArray {
        return Json.encodeToString(this).toByteArray()
    }

    companion object {
        fun deserialiser(aktorBytes: ByteArray): Aktor {
            return Json.decodeFromString<Aktor>(String(aktorBytes)).also { it.aktorId = sanitizeKey(it.aktorId) }
        }

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

@Serializable
data class Identifikator(
    @Id
    var idnummer: String = "",
    var type: Type = Type.FOLKEREGISTERIDENT,
    var gjeldende: Boolean = false,
    @Serializable(with = OppdatertSerializer::class)
    var oppdatert: OffsetDateTime = OffsetDateTime.now(),
)

object OppdatertSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    override fun serialize(
        encoder: Encoder,
        value: OffsetDateTime,
    ) {
        encoder.encodeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value))
    }
}

enum class Type {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}
