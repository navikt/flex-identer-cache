package no.nav.helse.flex.pdl.model

import no.nav.helse.flex.pdl.client.model.IdentInformasjon

data class PdlPerson(
    val identer: List<IdentInformasjon>,
) {
    val fnr: String? =
        identer.firstOrNull { it.gruppe == "FOLKEREGISTERIDENT" && !it.historisk }?.ident
}
