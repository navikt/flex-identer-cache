package no.nav.helse.flex.repository

import no.nav.helse.flex.logger
import no.nav.helse.flex.util.OBJECT_MAPPER
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktorService(private val redisTemplate: RedisTemplate<String, String>) {
    val log = logger()

    fun verifiserAktor(aktor: Aktor): Boolean {
        try {
            requireNotNull(aktor.aktorId) { "Aktor ID kan ikke være null" }
            val identifikatorer = requireNotNull(aktor.identifikatorer) { "Identifikatorer kan ikke være null" }

            identifikatorer.forEach { identifikator ->
                requireNotNull(identifikator.idnummer) { "ID-nummer kan ikke være null" }
                requireNotNull(identifikator.oppdatert) { "Oppdatert kan ikke være null" }
                requireNotNull(identifikator.type?.name) { "Type kan ikke være null" }
                requireNotNull(identifikator.gjeldende) { "Gjeldende kan ikke være null" }
            }
            return true
        } catch (e: Exception) {
            log.error("Feil på aktør med aktørId ${aktor.aktorId}", e.message)
            return false
        }
    }

    fun setValue(
        key: String,
        value: String,
    ) {
        redisTemplate.opsForValue().set(key, value)
    }

    @Transactional
    fun lagreFlereAktorer(aktorList: List<Aktor>) {
        redisTemplate.executePipelined { connection ->
            aktorList.forEach { aktor ->
                if (verifiserAktor(aktor)) {
                    val aktorString = OBJECT_MAPPER.writeValueAsString(aktor)
                    connection.stringCommands()
                        .set(aktor.aktorId!!.toByteArray(), aktorString.toByteArray())
                }
            }
            null // Return null since executePipelined expects a return type
        }
    }

    fun hentAktor(aktorId: String): Aktor? {
        val aktorString = redisTemplate.opsForValue().get(aktorId)
        return OBJECT_MAPPER.readValue(aktorString, Aktor::class.java)
    }
}
