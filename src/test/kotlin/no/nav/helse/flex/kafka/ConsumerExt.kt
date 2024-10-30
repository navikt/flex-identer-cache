package no.nav.helse.flex.kafka

import no.nav.helse.flex.repository.Aktor
import org.awaitility.Awaitility.await
import java.time.Duration

fun AktorConsumer.ventPaRecords(
    antall: Int,
    duration: Duration = Duration.ofSeconds(9),
): List<Aktor> {
    val factory =
        if (antall == 0) {
            // Må vente fullt ut, ikke opp til en tid siden vi vil se at ingen blir produsert
            await().during(duration)
        } else {
            await().atMost(duration)
        }

    val alle = ArrayList<Aktor>()
    runCatching {
        factory.until {
            while (buffer?.isNotEmpty() == true && alle.size < antall) {
                buffer?.poll()?.let { alle.add(it) }
            }
            alle.size == antall
        }
    }.onFailure {
        log.error("Forventet $antall meldinger på kafka, mottok ${alle.size}")
        throw it
    }

    return alle
}
