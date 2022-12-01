package no.nav.dagpenger.behov.brukernotifikasjon.db

import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.dagpenger.behov.brukernotifikasjon.config
import no.nav.dagpenger.behov.brukernotifikasjon.nais_app_name
import no.nav.dagpenger.behov.brukernotifikasjon.nais_namespace
import java.time.LocalDateTime
import java.util.UUID

interface NotifikasjonRepository {
    fun lagre(nøkkel: Nøkkel, beskjed: Beskjed)
}

data class Beskjed(private val tekst: String, private val opprettet: LocalDateTime, val sikkerhetsnivå: Int) {
    constructor(tekst: String, opprettet: LocalDateTime) : this(tekst, opprettet, 3)

    fun somMelding(): BeskjedInput = BeskjedInputBuilder().apply {
        withTekst(tekst)
        withTidspunkt(opprettet)
        withSikkerhetsnivaa(sikkerhetsnivå)
    }.build()
}

data class Nøkkel(private val eventId: String, private val ident: String, private val grupperingsId: String) {
    constructor(eventId: UUID, ident: String, grupperingsId: String) : this(eventId.toString(), ident, grupperingsId)

    fun somNøkkel(): NokkelInput = NokkelInputBuilder().apply {
        withEventId(eventId)
        withFodselsnummer(ident)
        withGrupperingsId(grupperingsId)
        withAppnavn(config[nais_app_name])
        withNamespace(config[nais_namespace])
    }.build()
}
