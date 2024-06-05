package no.nav.helse.flex.api

import no.nav.helse.flex.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.repository.AktorRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class FlexIdenterCacheApi(
    @Autowired
    private val aktorRepository: AktorRepository,
    private val clientIdValidation: ClientIdValidation,
) {
    @GetMapping("/api/v1/intern/identer")
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun hentIdenter(): Set<String> {
        clientIdValidation.validateClientId(
            ClientIdValidation.NamespaceAndApp(
                namespace = "flex",
                app = "flex-identer-cache",
            ),
        )
        return aktorRepository.finnAlleDistinctIder()
    }
}
