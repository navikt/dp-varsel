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

data class Beskjed(
    val tekst: String,
    val opprettet: LocalDateTime,
    val sikkerhetsnivå: Int,
    val eksternVarsling: Boolean
) {
    constructor(tekst: String) : this(tekst, LocalDateTime.now(), 3, false)
    constructor(tekst: String, opprettet: LocalDateTime) : this(tekst, opprettet, 3, false)
    constructor(tekst: String, opprettet: LocalDateTime, eksternVarsling: Boolean) : this(
        tekst,
        opprettet,
        3,
        eksternVarsling
    )

    fun somMelding(): BeskjedInput = BeskjedInputBuilder().apply {
        withTekst(tekst)
        withTidspunkt(opprettet)
        withSikkerhetsnivaa(sikkerhetsnivå)
        withEksternVarsling(eksternVarsling)
    }.build()
}

data class Nøkkel(private val eventId: String, private val ident: String) {
    constructor(ident: String) : this(UUID.randomUUID(), ident)
    constructor(eventId: UUID, ident: String) : this(eventId.toString(), ident)

    fun somNøkkel(): NokkelInput = NokkelInputBuilder().apply {
        withEventId(eventId)
        withFodselsnummer(ident)
        withGrupperingsId("deprecated")
        withAppnavn(config[nais_app_name])
        withNamespace(config[nais_namespace])
    }.build()
}
