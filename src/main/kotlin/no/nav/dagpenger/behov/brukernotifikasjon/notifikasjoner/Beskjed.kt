package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal.SMS
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.dagpenger.behov.brukernotifikasjon.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.NotifikasjonKommando
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID

internal typealias BeskjedTopic = NotifikasjonTopic<BeskjedInput>

internal data class Beskjed constructor(
    override val eventId: UUID,
    private val ident: Ident,
    private val tekst: String,
    private val opprettet: LocalDateTime,
    private val sikkerhetsnivå: Int,
    private val eksternVarsling: Boolean,
    private val link: URL?
) : NotifikasjonKommando(), NotifikasjonMelding<BeskjedInput> {
    constructor(ident: Ident, tekst: String, eksternVarsling: Boolean = false) : this(
        UUID.randomUUID(),
        ident,
        tekst,
        LocalDateTime.now(),
        3,
        eksternVarsling,
        null
    )

    constructor(eventId: UUID, ident: Ident, tekst: String) : this(
        eventId,
        ident,
        tekst,
        LocalDateTime.now(),
        3,
        false,
        null
    )

    constructor(ident: Ident, tekst: String, sikkerhetsnivå: Int, eksternVarsling: Boolean, link: URL?) : this(
        UUID.randomUUID(),
        ident,
        tekst,
        LocalDateTime.now(),
        sikkerhetsnivå,
        eksternVarsling,
        link
    )

    constructor(eventId: UUID, ident: Ident, tekst: String, opprettet: LocalDateTime) : this(
        eventId,
        ident,
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
    override fun somInput(): BeskjedInput = BeskjedInputBuilder().apply {
        withTekst(tekst)
        withTidspunkt(opprettet)
        withSikkerhetsnivaa(sikkerhetsnivå)
        withLink(link)
        withEksternVarsling(eksternVarsling)
        if (eksternVarsling) {
            withPrefererteKanaler(SMS)
        }
    }.build()

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
