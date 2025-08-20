package no.nav.dagpenger.behov.brukernotifikasjon.kafka

import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import java.util.UUID

internal interface NotifikasjonMelding {
    fun somInput(): String
}

internal data class Nøkkel(internal val eventId: UUID, internal val ident: Ident)
