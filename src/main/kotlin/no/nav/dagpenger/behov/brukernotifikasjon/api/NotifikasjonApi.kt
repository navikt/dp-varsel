package no.nav.dagpenger.behov.brukernotifikasjon.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behov.brukernotifikasjon.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.NotifikasjonBroadcaster
import no.nav.dagpenger.behov.brukernotifikasjon.Notifikasjoner
import no.nav.dagpenger.behov.brukernotifikasjon.api.plugins.configureSerialization
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import java.net.URL

internal fun Application.notifikasjonApi(
    notifikasjoner: Notifikasjoner,
    notifikasjonBroadcaster: NotifikasjonBroadcaster
) {
    configureSerialization()

    routing {
        route("beskjed") {
            get {}
            get("{id?}") {}
            post<PostBeskjed> { body ->
                notifikasjoner.send(body.somKommando())
            }
        }

        route("internal") {
            post<PostBeskjedTilAlleIdenter>("broadcast") { body ->
                notifikasjonBroadcaster.sendBeskjedTilAlleIdenterISecreten(body.dryRun)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

data class PostBeskjed(
    val ident: String,
    val tekst: String,
    val eksternVarsling: Boolean = false,
    val link: String? = null
) {
    internal fun somKommando() =
        Beskjed(
            ident = Ident(ident),
            tekst = tekst,
            sikkerhetsnivå = 3,
            eksternVarsling = eksternVarsling,
            link = link?.let { URL(it) }
        )
}

data class PostBeskjedTilAlleIdenter(val dryRun: Boolean = true)
