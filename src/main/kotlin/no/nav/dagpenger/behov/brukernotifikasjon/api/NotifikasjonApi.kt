package no.nav.dagpenger.behov.brukernotifikasjon.api

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behov.brukernotifikasjon.Notifikasjoner
import no.nav.dagpenger.behov.brukernotifikasjon.api.plugins.configureSerialization
import no.nav.dagpenger.behov.brukernotifikasjon.db.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.db.Nøkkel
import java.util.UUID

internal fun Application.notifikasjonApi(notifikasjoner: Notifikasjoner) {
    configureSerialization()

    routing {
        route("beskjed") {
            get {}
            get("{id?}") {}
            post<Beskjed> {
                val beskjed = call.receive<Beskjed>()
                notifikasjoner.send(Nøkkel(UUID.randomUUID(), "ident", "QUE?"), beskjed)
            }
        }
    }
}
