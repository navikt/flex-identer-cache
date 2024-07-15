package no.nav.helse.flex.repository

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.io.AvroDecodeFormat
import com.github.avrokotlin.avro4k.io.AvroEncodeFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
@Table("aktor")
data class Aktor(
    @Id
    var aktorId: String,
    @MappedCollection(idColumn = "aktor_id", keyColumn = "aktor_id")
    var identifikatorer: List<Identifikator> = mutableListOf(),
) {
    fun serialiser(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val schema = Avro.default.schema(serializer())
        val output =
            Avro.default.openOutputStream(serializer()) {
                encodeFormat = AvroEncodeFormat.Binary
                this.schema = schema
            }.to(byteArrayOutputStream)
        output.write(this)
        output.close()
        return byteArrayOutputStream.toByteArray()
    }
}

fun ByteArray.deserialiserTilAktor(): Aktor {
    val byteArrayInputStream = ByteArrayInputStream(this)
    val schema = Avro.default.schema(Aktor.serializer())
    val input =
        Avro.default.openInputStream(Aktor.serializer()) {
            decodeFormat = AvroDecodeFormat.Binary(schema, schema)
        }.from(byteArrayInputStream)
    return input.nextOrThrow()
}

@Serializable
@Table("identifikator")
data class Identifikator(
    @Id
    var idnummer: String,
    var type: String,
    var gjeldende: Boolean,
    @Serializable(with = OppdatertSerializer::class)
    var oppdatert: OffsetDateTime? = null,
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

enum class IdentType {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}
