package no.nav.helse.flex.repository

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Transactional
@Repository
interface IdenterRepository : CrudRepository<IdenterDbRecord, String> {
    @Query("SELECT DISTINCT * FROM identer f")
    fun finnAlleDistinctTags(): List<String?>

    // Lagrer en ident i databasen
    @Modifying
    @Query("INSERT INTO identer (id, opprettet, type, gjeldende) VALUES (:id, :opprettet, :type, :gjeldende)")
    fun lagre(
        @Param("id") id: String,
        @Param("opprettet") opprettet: OffsetDateTime,
        @Param("type") type: String,
        @Param("gjeldende") gjeldende: Boolean,
    ): Int
}

@Table("identer")
data class IdenterDbRecord(
    @Id
    val id: String? = null,
    val opprettet: OffsetDateTime,
    val type: String,
    val gjeldende: Boolean = true,
)
