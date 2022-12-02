package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.dagpenger.behov.brukernotifikasjon.NotifikasjonKommando
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import java.time.LocalDateTime
import java.util.UUID

internal typealias OppgaveTopic = NotifikasjonTopic<OppgaveInput>

internal data class Oppgave(
    override val eventId: UUID,
    private val ident: String,
    private val tekst: String,
    private val asLocalDateTime: LocalDateTime
) :
    NotifikasjonKommando() {
    override fun getNøkkel() = Nøkkel(ident)
    override fun getMelding() = OppgaveMelding(tekst)
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(getNøkkel(), getMelding())
}

internal data class OppgaveMelding(
    private val tekst: String,
    private val opprettet: LocalDateTime,
    private val sikkerhetsnivå: Int,
    private val eksternVarsling: Boolean
) : NotifikasjonMelding<OppgaveInput> {
    constructor(tekst: String) : this(tekst, LocalDateTime.now(), 3, false)
    constructor(tekst: String, opprettet: LocalDateTime) : this(tekst, opprettet, 3, false)
    constructor(tekst: String, opprettet: LocalDateTime, eksternVarsling: Boolean) : this(
        tekst,
        opprettet,
        3,
        eksternVarsling
    )

    override fun somInput(): OppgaveInput = OppgaveInputBuilder().apply {
        withTekst(tekst)
        withTidspunkt(opprettet)
        withSikkerhetsnivaa(sikkerhetsnivå)
        withEksternVarsling(eksternVarsling)
    }.build()
}
