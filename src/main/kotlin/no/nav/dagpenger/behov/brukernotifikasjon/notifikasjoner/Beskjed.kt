package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.dagpenger.behov.brukernotifikasjon.NotifikasjonKommando
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import java.time.LocalDateTime
import java.util.UUID

internal typealias BeskjedTopic = NotifikasjonTopic<BeskjedInput>

internal data class Beskjed(
    override val eventId: UUID,
    private val ident: String,
    private val tekst: String,
    private val oppprettet: LocalDateTime
) :
    NotifikasjonKommando() {
    constructor(ident: String, tekst: String) : this(UUID.randomUUID(), ident, tekst, LocalDateTime.now())

    override fun getNøkkel() = Nøkkel(eventId, ident)
    override fun getMelding() = BeskjedMelding(tekst, oppprettet)
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(getNøkkel(), getMelding())
}

internal data class BeskjedMelding(
    internal val tekst: String,
    internal val opprettet: LocalDateTime,
    internal val sikkerhetsnivå: Int,
    internal val eksternVarsling: Boolean
) : NotifikasjonMelding<BeskjedInput> {
    constructor(tekst: String) : this(tekst, LocalDateTime.now(), 3, false)
    constructor(tekst: String, opprettet: LocalDateTime) : this(tekst, opprettet, 3, false)
    constructor(tekst: String, opprettet: LocalDateTime, eksternVarsling: Boolean) : this(
        tekst,
        opprettet,
        3,
        eksternVarsling
    )

    override fun somInput(): BeskjedInput = BeskjedInputBuilder().apply {
        withTekst(tekst)
        withTidspunkt(opprettet)
        withSikkerhetsnivaa(sikkerhetsnivå)
        withEksternVarsling(eksternVarsling)
    }.build()
}
