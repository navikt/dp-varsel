package no.nav.dagpenger.behov.brukernotifikasjon.api

import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behov.brukernotifikasjon.Notifikasjoner
import no.nav.dagpenger.behov.brukernotifikasjon.api.plugins.configureSerialization
import no.nav.dagpenger.behov.brukernotifikasjon.db.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.db.Nøkkel
import java.time.LocalDateTime
import java.util.UUID

internal fun Application.notifikasjonApi(notifikasjoner: Notifikasjoner) {
    configureSerialization()

    routing {
        route("beskjed") {
            get {}
            get("{id?}") {}
            post<PostBeskjed> { body ->
                notifikasjoner.send(Nøkkel(UUID.randomUUID(), body.ident), body.somBeskjed())
            }
        }
    }
}

data class PostBeskjed(
    val ident: String,
    val tekst: String,
    val eksternVarsling: Boolean = false
) {
    fun somBeskjed() = Beskjed(tekst, LocalDateTime.now(), eksternVarsling)
}
