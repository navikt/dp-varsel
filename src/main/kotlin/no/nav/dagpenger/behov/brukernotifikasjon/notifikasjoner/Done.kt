package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.NotifikasjonKommando
import java.time.LocalDateTime
import java.util.*

internal typealias DoneTopic = NotifikasjonTopic<DoneInput>

internal data class Done(
    private val ident: Ident,
    override val eventId: UUID,
    private val deaktiveringstidspunkt: LocalDateTime,
    private val eventtype: Eventtype
) : NotifikasjonKommando(), NotifikasjonMelding<DoneInput> {

    constructor(ident: Ident, eventId: UUID, eventtype: Eventtype) : this(
        ident,
        eventId,
        LocalDateTime.now(),
        eventtype
    )

    override fun getNøkkel() = Nøkkel(eventId, ident)
    override fun getMelding() = this
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(this)
    override fun somInput(): DoneInput = DoneInputBuilder().apply {
        withTidspunkt(deaktiveringstidspunkt)

    }.build()

    fun getSnapshot() = DoneSnapshot(this)

    internal data class DoneSnapshot(
        val deaktiveringstidspunkt: LocalDateTime,
        val eventtype: Eventtype
    ) {
        constructor(done: Done) : this(done.deaktiveringstidspunkt, done.eventtype)
    }

    internal enum class Eventtype {
        BESKJED, OPPGAVE
    }

}
