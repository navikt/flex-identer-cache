package no.nav.helse.flex.api

import no.nav.helse.flex.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.model.Aktor
import no.nav.helse.flex.model.AktorService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import no.nav.helse.flex.config.OIDCIssuer.AZUREATOR

@RestController
class FlexIdenterCacheApi(
    @Autowired
    private val aktorService: AktorService,
    private val clientIdValidation: ClientIdValidation,
) {
    @GetMapping("/api/v1/identer/{aktorId}")
    @ResponseBody
    @ProtectedWithClaims(issuer = AZUREATOR)
    fun hentIdenterForAktor(@PathVariable aktorId: String): Aktor? {
        clientIdValidation.validateClientId(
            ClientIdValidation.NamespaceAndApp(
                namespace = "flex",
                app = "sykepengesoknad-backend",
            ),
        )
        return aktorService.hentAktor(aktorId)
    }
}
