package no.nav.helse.flex.repository

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
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

@Table("aktor")
data class Aktor(
    @Id
    var aktorId: String,
    @MappedCollection(idColumn = "aktor_id", keyColumn = "aktor_id")
    var identifikatorer: List<Identifikator> = mutableListOf(),
)

@Table("identifikator")
data class Identifikator(
    @Id
    var idnummer: String,
    var type: String,
    var gjeldende: Boolean,
    var oppdatert: OffsetDateTime,
)

enum class IdentType {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}
