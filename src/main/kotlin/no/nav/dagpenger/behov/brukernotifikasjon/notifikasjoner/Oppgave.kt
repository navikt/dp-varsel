package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.NotifikasjonKommando
import no.nav.tms.varsel.action.EksternKanal.SMS
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Sensitivitet.High
import no.nav.tms.varsel.action.Sensitivitet.Substantial
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype.Oppgave
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

internal data class Oppgave(
    private val ident: Ident,
    override val eventId: UUID,
    private val tekst: String,
    private val opprettet: LocalDateTime,
    private val sikkerhetsnivå: Int,
    private val eksternVarsling: Boolean,
    private val link: URL,
    private val søknadId: UUID,
    private val deaktiveringstidspunkt: LocalDateTime?,
    private val deaktiveringsgrunn: Done.Grunn?,
    private val synligFramTil: LocalDateTime,
    private val aktiv: Boolean = true,
    private val eksternVarslingTekst: String? = null,
) : NotifikasjonKommando(), NotifikasjonMelding {
    constructor(
        ident: Ident,
        eventId: UUID,
        tekst: String,
        opprettet: LocalDateTime,
        link: URL,
        søknadId: UUID,
        synligFramTil: LocalDateTime,
        eksternVarsling: Boolean = false,
        eksternVarslingTekst: String? = null,
    ) : this(
        ident = ident,
        eventId = eventId,
        tekst = tekst,
        opprettet = opprettet,
        sikkerhetsnivå = 3,
        eksternVarsling = eksternVarsling,
        link = link,
        søknadId = søknadId,
        deaktiveringstidspunkt = null,
        deaktiveringsgrunn = null,
        synligFramTil = synligFramTil,
        eksternVarslingTekst = eksternVarslingTekst
    )

    override fun getNøkkel() = Nøkkel(eventId, ident)
    override fun getMelding() = this
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(this)

    override fun somInput() = VarselActionBuilder.opprett {
        type = Oppgave
        varselId = eventId.toString()
        sensitivitet = sikkerhetsnivåTilSensitivitet()
        ident = this@Oppgave.ident.ident
        tekst = Tekst(
            spraakkode = "nb",
            tekst = this@Oppgave.tekst,
            default = true
        )
        link = this@Oppgave.link.toString()
        aktivFremTil = synligFramTil.atZone(ZoneId.of("Europe/Oslo"))
        if (this@Oppgave.eksternVarsling) eksternVarsling {
            preferertKanal = SMS
            smsVarslingstekst = eksternVarslingTekst
        }
    }

    private fun sikkerhetsnivåTilSensitivitet(): Sensitivitet = when (sikkerhetsnivå) {
        4 -> High
        3 -> Substantial
        else -> throw IllegalArgumentException("Ugyldig sikkerhetsnivå: $sikkerhetsnivå")
    }

    fun getSnapshot() = OppgaveSnapshot(this)

    internal data class OppgaveSnapshot(
        val eventId: UUID,
        val ident: Ident,
        val tekst: String,
        val opprettet: LocalDateTime,
        val sikkerhetsnivå: Int,
        val eksternVarsling: Boolean,
        val link: URL,
        val søknadId: UUID,
        val aktiv: Boolean,
        val deaktiveringstidspunkt: LocalDateTime?,
        val deaktiveringsgrunn: Done.Grunn?,
        val synligFramTil: LocalDateTime,
    ) {
        constructor(oppgave: Oppgave) : this(
            oppgave.eventId,
            oppgave.ident,
            oppgave.tekst,
            oppgave.opprettet,
            oppgave.sikkerhetsnivå,
            oppgave.eksternVarsling,
            oppgave.link,
            oppgave.søknadId,
            oppgave.aktiv,
            oppgave.deaktiveringstidspunkt,
            oppgave.deaktiveringsgrunn,
            oppgave.synligFramTil
        )
    }
}
