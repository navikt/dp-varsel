package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.KafkaTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.*
import org.apache.avro.specific.SpecificRecord
import java.util.*

internal typealias NotifikasjonTopic<T> = KafkaTopic<NokkelInput, T>

internal data class Ident(val ident: String)

internal class Notifikasjoner(
    private val repository: NotifikasjonRepository,
    private val beskjedTopic: BeskjedTopic,
    private val oppgaveTopic: OppgaveTopic,
    private val doneTopic: DoneTopic
) {
    fun send(kommando: Beskjed) {
        kommando.lagre(repository)
        kommando.send(beskjedTopic)
    }

    fun send(kommando: Oppgave) {
        kommando.lagre(repository)
        kommando.send(oppgaveTopic)
    }

    fun send(kommando: Done) {
        kommando.lagre(repository)
        kommando.send(doneTopic)
    }
}

internal abstract class NotifikasjonKommando {
    protected abstract val eventId: UUID
    abstract fun getNøkkel(): Nøkkel
    protected abstract fun getMelding(): NotifikasjonMelding<*>
    fun <T : SpecificRecord> send(topic: NotifikasjonTopic<T>) =
        @Suppress("UNCHECKED_CAST")
        topic.publiser(getNøkkel().somInput(), getMelding().somInput() as T)

    abstract fun lagre(repository: NotifikasjonRepository): Boolean
}
