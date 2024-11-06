package no.nav.helse.flex.api

import no.nav.helse.flex.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.config.OIDCIssuer.AZUREATOR
import no.nav.helse.flex.model.Aktor
import no.nav.helse.flex.model.AktorService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class FlexIdenterCacheApi(
    @Autowired
    private val aktorService: AktorService,
    private val clientIdValidation: ClientIdValidation,
) {
    @PostMapping("/api/v1/identer/aktor")
    @ResponseBody
    @ProtectedWithClaims(issuer = AZUREATOR)
    fun hentIdenterForAktor(
        @RequestParam aktorId: String,
    ): Aktor? {
        clientIdValidation.validateClientId(
            ClientIdValidation.NamespaceAndApp(
                namespace = "flex",
                app = "sykepengesoknad-backend",
            ),
        )
        return aktorService.hentAktor(aktorId)
    }
}
