package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.NotifikasjonKommando
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.LocalDateTime
import java.util.UUID

internal data class Done(
    private val ident: Ident,
    override val eventId: UUID,
    // TODO: Denne er deprecated i ny løsning av brukervarsel
    private val deaktiveringstidspunkt: LocalDateTime,
    private val grunn: Grunn,
    private val eventtype: Eventtype
) : NotifikasjonKommando(), NotifikasjonMelding {

    override fun getNøkkel() = Nøkkel(eventId, ident)
    override fun getMelding() = this
    override fun lagre(repository: NotifikasjonRepository) = repository.lagre(this)

    override fun somInput() = VarselActionBuilder.inaktiver {
        varselId = eventId.toString()
    }

    fun getSnapshot() = DoneSnapshot(this)

    internal data class DoneSnapshot(
        val tidspunkt: LocalDateTime,
        val grunn: Grunn,
        val eventtype: Eventtype
    ) {
        constructor(done: Done) : this(done.deaktiveringstidspunkt, done.grunn, done.eventtype)
    }

    internal enum class Eventtype {
        BESKJED, OPPGAVE
    }

    internal enum class Grunn {
        FERDIG,
        VEDTAK_ELLER_AVSLAG,
        UTLOPT
    }
}
