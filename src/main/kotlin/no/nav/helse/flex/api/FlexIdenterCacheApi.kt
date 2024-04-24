package no.nav.helse.flex.api

import no.nav.helse.flex.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.repository.IdenterRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*

@RestController
class FlexIdenterCacheApi(
    private val identerRepository: IdenterRepository,
    private val clientIdValidation: ClientIdValidation,
) {
    @GetMapping("/api/v1/intern/identer")
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun hentAlleTags(): String {
        clientIdValidation.validateClientId(
            ClientIdValidation.NamespaceAndApp(
                namespace = "flex",
                app = "flex-identer-cache",
            ),
        )
        return "Hello, world"
    }
}
