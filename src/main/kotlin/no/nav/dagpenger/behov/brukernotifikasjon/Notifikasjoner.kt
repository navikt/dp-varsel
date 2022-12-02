package no.nav.dagpenger.behov.brukernotifikasjon

import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.BeskjedTopic
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.OppgaveTopic
import org.apache.avro.specific.SpecificRecord
import java.util.UUID

internal class Notifikasjoner(
    private val repository: NotifikasjonRepository,
    private val beskjedTopic: BeskjedTopic,
    private val oppgaveTopic: OppgaveTopic
) {
    fun send(kommando: Beskjed) {
        kommando.lagre(repository)
        kommando.send(beskjedTopic)
    }

    fun send(kommando: Oppgave) {
        kommando.lagre(repository)
        kommando.send(oppgaveTopic)
    }
}

internal abstract class NotifikasjonKommando {
    protected abstract val eventId: UUID
    abstract fun getNøkkel(): Nøkkel
    abstract fun getMelding(): NotifikasjonMelding<*>
    fun <T : SpecificRecord> send(topic: NotifikasjonTopic<T>) =
        @Suppress("UNCHECKED_CAST")
        topic.publiser(getNøkkel(), getMelding() as NotifikasjonMelding<T>)

    abstract fun lagre(repository: NotifikasjonRepository): Boolean
}
