package no.nav.helse.flex.repository

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

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

data class Aktor(
    @Id
    var aktorId: String = "",
    @MappedCollection(idColumn = "aktor_id", keyColumn = "aktor_id")
    var identifikatorer: List<Identifikator> = mutableListOf(),
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
    var idnummer: String = "",
    var type: Type = Type.FOLKEREGISTERIDENT,
    var gjeldende: Boolean = false,
    var oppdatert: OffsetDateTime = OffsetDateTime.now(),
)

enum class Type {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}
