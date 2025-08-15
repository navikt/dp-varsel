package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.NotifikasjonKommando
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.NotifikasjonTopic
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID

internal typealias BeskjedTopic = NotifikasjonTopic<BeskjedInput>

internal data class Beskjed constructor(
    private val ident: Ident,
    override val eventId: UUID,
    private val tekst: String,
    private val opprettet: LocalDateTime,
    private val sikkerhetsnivå: Int,
    private val eksternVarsling: Boolean,
    private val link: URL? // TODO: Ta inn String
) : NotifikasjonKommando(), NotifikasjonMelding<String> {
    constructor(ident: Ident, tekst: String, eksternVarsling: Boolean = false) : this(
        ident,
        UUID.randomUUID(),
        tekst,
        LocalDateTime.now(),
        3,
        eksternVarsling,
        null
    )

    constructor(ident: Ident, eventId: UUID, tekst: String) : this(
        ident,
        eventId,
        tekst,
        LocalDateTime.now(),
        3,
        false,
        null
    )

    constructor(ident: Ident, tekst: String, sikkerhetsnivå: Int, eksternVarsling: Boolean, link: URL?) : this(
        ident,
        UUID.randomUUID(),
        tekst,
        LocalDateTime.now(),
        sikkerhetsnivå,
        eksternVarsling,
        link
    )

    constructor(ident: Ident, eventId: UUID, tekst: String, opprettet: LocalDateTime) : this(
        ident,
        eventId,
        tekst,
        opprettet,
        3,
        false,
        null
    )

    override fun getNøkkel() = Nøkkel(eventId, ident)
    override fun getMelding() = this
    fun getSnapshot() = BeskjedSnapshot(this)
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(this)

    override fun somInput() = VarselActionBuilder.opprett {
        type = Varseltype.Beskjed
        varselId = eventId.toString()
        sensitivitet = sikkerhetsnivåTilSensitivitet()
        ident = this@Beskjed.ident.ident
        tekst = Tekst(
            spraakkode = "nb",
            tekst = this@Beskjed.tekst,
            default = true
        )
        link = this@Beskjed.link?.toString()
        if (this@Beskjed.eksternVarsling) eksternVarsling {
            preferertKanal = EksternKanal.SMS
        }
    }

    private fun sikkerhetsnivåTilSensitivitet (): Sensitivitet = when (sikkerhetsnivå) {
        4 -> Sensitivitet.High
        3 -> Sensitivitet.Substantial
        else -> throw IllegalArgumentException("Ugyldig sikkerhetsnivå: $sikkerhetsnivå")
    }


    internal data class BeskjedSnapshot(
        val eventId: UUID,
        val ident: Ident,
        val tekst: String,
        val opprettet: LocalDateTime,
        val sikkerhetsnivå: Int,
        val eksternVarsling: Boolean,
        val link: URL?
    ) {
        constructor(beskjed: Beskjed) : this(
            beskjed.eventId,
            beskjed.ident,
            beskjed.tekst,
            beskjed.opprettet,
            beskjed.sikkerhetsnivå,
            beskjed.eksternVarsling,
            beskjed.link
        )
    }
}
