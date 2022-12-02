package no.nav.dagpenger.behov.brukernotifikasjon.api

import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behov.brukernotifikasjon.Notifikasjoner
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.api.plugins.configureSerialization
import java.time.LocalDateTime
import java.util.UUID

internal fun Application.notifikasjonApi(notifikasjoner: Notifikasjoner) {
    configureSerialization()

    routing {
        route("beskjed") {
            get {}
            get("{id?}") {}
            post<PostBeskjed> { body ->
                notifikasjoner.send(body.somKommando())
            }
        }
    }
}

data class PostBeskjed(
    val ident: String,
    val tekst: String,
    val eksternVarsling: Boolean = false
) {
    internal fun somKommando() = Beskjed(UUID.randomUUID(), ident, tekst, LocalDateTime.now())
}
