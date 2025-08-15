package no.nav.dagpenger.behov.brukernotifikasjon.kafka

import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.dagpenger.behov.brukernotifikasjon.config
import no.nav.dagpenger.behov.brukernotifikasjon.nais_app_name
import no.nav.dagpenger.behov.brukernotifikasjon.nais_namespace
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import java.util.UUID

// TODO: Er nå Any for å funke med ny String-input fra Beskjed og SpecificRecord fra de andre typene
//  Denne kan sikkert endres mer, eller fjernes helt når alle typene er oppdatert
internal interface NotifikasjonMelding<T : Any> {
    fun somInput(): T
}

internal data class Nøkkel(internal val eventId: UUID, internal val ident: Ident) {
    constructor(ident: Ident) : this(UUID.randomUUID(), ident)

    fun somInput(): NokkelInput = NokkelInputBuilder().apply {
        withEventId(eventId.toString())
        withFodselsnummer(ident.ident)
        withGrupperingsId("deprecated")
        withAppnavn(config[nais_app_name])
        withNamespace(config[nais_namespace])
    }.build()
}
