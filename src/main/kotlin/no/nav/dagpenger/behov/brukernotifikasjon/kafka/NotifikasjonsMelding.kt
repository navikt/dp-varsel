package no.nav.dagpenger.behov.brukernotifikasjon.kafka

import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.dagpenger.behov.brukernotifikasjon.config
import no.nav.dagpenger.behov.brukernotifikasjon.nais_app_name
import no.nav.dagpenger.behov.brukernotifikasjon.nais_namespace
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import org.apache.avro.specific.SpecificRecord
import java.util.UUID

internal interface NotifikasjonMelding<T : SpecificRecord> {
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
