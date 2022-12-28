package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.dagpenger.behov.brukernotifikasjon.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.NotifikasjonKommando
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID

internal typealias OppgaveTopic = NotifikasjonTopic<OppgaveInput>

internal data class Oppgave(
    override val eventId: UUID,
    private val ident: Ident,
    private val tekst: String,
    private val opprettet: LocalDateTime,
    private val sikkerhetsnivå: Int,
    private val eksternVarsling: Boolean,
    private val link: URL
) : NotifikasjonKommando(), NotifikasjonMelding<OppgaveInput> {
    constructor(ident: Ident, tekst: String, link: URL) : this(
        UUID.randomUUID(),
        ident,
        tekst,
        LocalDateTime.now(),
        3,
        false,
        link
    )

    constructor(ident: Ident, eventId: UUID, tekst: String, opprettet: LocalDateTime, link : URL) : this(
        eventId,
        ident,
        tekst,
        opprettet,
        3,
        false,
        link
    )

    constructor(ident: Ident, eventId: UUID, tekst: String, opprettet: LocalDateTime, eksternVarsling: Boolean, link : URL) : this(
        eventId,
        ident,
        tekst,
        opprettet,
        3,
        eksternVarsling,
        link
    )

    override fun getNøkkel() = Nøkkel(eventId, ident)
    override fun getMelding() = this
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(this)
    override fun somInput(): OppgaveInput = OppgaveInputBuilder().apply {
        withTekst(tekst)
        withTidspunkt(opprettet)
        withSikkerhetsnivaa(sikkerhetsnivå)
        withEksternVarsling(eksternVarsling)
        if(eksternVarsling) {
            withPrefererteKanaler(PreferertKanal.SMS)
        }
        withLink(link)
    }.build()
}
