package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.dagpenger.behov.brukernotifikasjon.Ident
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
    private val ident: Ident,
    private val tekst: String,
    private val opprettet: LocalDateTime,
    private val sikkerhetsnivå: Int,
    private val eksternVarsling: Boolean
) : NotifikasjonKommando(), NotifikasjonMelding<OppgaveInput> {
    constructor(tekst: String, ident: Ident) : this(UUID.randomUUID(), ident, tekst, LocalDateTime.now(), 3, false)
    constructor(tekst: String, ident: Ident, opprettet: LocalDateTime) : this(
        UUID.randomUUID(),
        ident,
        tekst,
        opprettet,
        3,
        false
    )

    constructor(tekst: String, ident: Ident, opprettet: LocalDateTime, eksternVarsling: Boolean) : this(
        UUID.randomUUID(),
        ident,
        tekst,
        opprettet,
        3,
        eksternVarsling
    )

    override fun getNøkkel() = Nøkkel(ident)
    override fun getMelding() = this
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(this)
    override fun somInput(): OppgaveInput = OppgaveInputBuilder().apply {
        withTekst(tekst)
        withTidspunkt(opprettet)
        withSikkerhetsnivaa(sikkerhetsnivå)
        withEksternVarsling(eksternVarsling)
    }.build()
}
