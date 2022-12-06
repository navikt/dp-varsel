package no.nav.dagpenger.behov.brukernotifikasjon.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behov.brukernotifikasjon.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.NotifikasjonBroadcaster
import no.nav.dagpenger.behov.brukernotifikasjon.Notifikasjoner
import no.nav.dagpenger.behov.brukernotifikasjon.api.plugins.configureSerialization
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import java.time.LocalDateTime
import java.util.UUID

internal fun Application.notifikasjonApi(notifikasjoner: Notifikasjoner, notifikasjonBroadcaster: NotifikasjonBroadcaster) {
    configureSerialization()

    routing {
        route("beskjed") {
            get {}
            get("{id?}") {}
            post<PostBeskjedTilAlleIdenter>("broadcast") { body ->
                notifikasjonBroadcaster.sendBeskjedTilAlleIdenterISecreten(body.dryRun)
                call.respond(HttpStatusCode.OK)
            }
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
    internal fun somKommando() =
        Beskjed(UUID.randomUUID(), Ident(ident), tekst, LocalDateTime.now(), 3, eksternVarsling = eksternVarsling)
}

data class PostBeskjedTilAlleIdenter(val dryRun : Boolean = true)
