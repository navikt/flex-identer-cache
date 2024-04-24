package no.nav.helse.flex.repository

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface IdenterRepository : CrudRepository<IdenterDbRecord, String> {
    @Query("SELECT DISTINCT * FROM identer f")
    fun finnAlleDistinctTags(): List<String?>
}

@Table("identer")
data class IdenterDbRecord(
    @Id
    val id: String? = null,
//    val opprettet: OffsetDateTime,
//    val feedbackJson: String,
//    val team: String,
//    val app: String? = null,
//    val tags: String? = null,
)

data class TeamApp(
    val team: String,
    val app: String? = null,
)
