package no.nav.helse.flex.model

import no.nav.helse.flex.util.OBJECT_MAPPER
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AktorService(private val redisTemplate: RedisTemplate<String, String>) {
    @Transactional
    fun lagreFlereAktorer(aktorList: List<Aktor>) {
        redisTemplate.executePipelined { connection ->
            aktorList.forEach { aktor ->
                val aktorString = OBJECT_MAPPER.writeValueAsString(aktor)
                connection.stringCommands()
                    .set(aktor.aktorId.toByteArray(), aktorString.toByteArray())

                fun lagreIdentForAktor(aktorId: String?) {
                    aktor.identifikatorer.forEach { identifikator ->
                        identifikator.type.name
                        val identId = identifikator.idnummer

                        val identKey = "ident:$identId"
                        connection.stringCommands()
                            .set(identKey.toByteArray(), aktorId!!.toByteArray())
                    }
                }

                lagreIdentForAktor(aktor.aktorId)
            }
            null // Return null since executePipelined expects a return type
        }
    }

    fun hentAktor(aktorId: String): Aktor? {
        val aktorString =
            redisTemplate.opsForValue().get(aktorId)
                ?: return null

        return OBJECT_MAPPER.readValue(aktorString, Aktor::class.java)
    }

    fun hentAktorForIdent(ident: String): Aktor? {
        val identKey = "ident:$ident"
        val aktorId =
            redisTemplate.opsForValue().get(identKey)
                ?: return null

        return hentAktor(aktorId)
    }
}
