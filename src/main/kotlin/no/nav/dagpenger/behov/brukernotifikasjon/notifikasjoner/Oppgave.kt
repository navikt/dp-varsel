package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.NotifikasjonKommando
import java.net.URL
import java.time.LocalDateTime
import java.util.*

internal typealias OppgaveTopic = NotifikasjonTopic<OppgaveInput>

internal data class Oppgave(
    override val eventId: UUID,
    private val ident: Ident,
    private val tekst: String,
    private val opprettet: LocalDateTime,
    private val sikkerhetsnivå: Int,
    private val eksternVarsling: Boolean,
    private val link: URL,
    private val søknadId : UUID
) : NotifikasjonKommando(), NotifikasjonMelding<OppgaveInput> {

    constructor(ident: Ident, eventId: UUID, tekst: String, opprettet: LocalDateTime, link: URL, søknadId: UUID) : this(
        eventId,
        ident,
        tekst,
        opprettet,
        3,
        false,
        link,
        søknadId
    )

    constructor(eventId: UUID, ident: Ident, link: URL, tekst: String, søknadId: UUID) : this(
        eventId,
        ident,
        tekst,
        LocalDateTime.now(),
        3,
        false,
        link,
        søknadId
    )

    override fun getNøkkel() = Nøkkel(eventId, ident)
    override fun getMelding() = this
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(this)
    override fun somInput(): OppgaveInput = OppgaveInputBuilder().apply {
        withTekst(tekst)
        withTidspunkt(opprettet)
        withSikkerhetsnivaa(sikkerhetsnivå)
        withEksternVarsling(eksternVarsling)
        if (eksternVarsling) {
            withPrefererteKanaler(PreferertKanal.SMS)
        }
        withLink(link)
    }.build()

    fun getSnapshot() = OppgaveSnapshot(this)

    internal data class OppgaveSnapshot(
        val eventId: UUID,
        val ident: Ident,
        val tekst: String,
        val opprettet: LocalDateTime,
        val sikkerhetsnivå: Int,
        val eksternVarsling: Boolean,
        val link: URL,
        val søknadId: UUID
    ) {
        constructor(oppgave: Oppgave) : this(
            oppgave.eventId,
            oppgave.ident,
            oppgave.tekst,
            oppgave.opprettet,
            oppgave.sikkerhetsnivå,
            oppgave.eksternVarsling,
            oppgave.link,
            oppgave.søknadId
        )
    }

}
