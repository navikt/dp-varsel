package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.KafkaTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import java.util.UUID

// TODO: Denne kan fjernes, siden det bare er én type topic, og vi trenger bare id som nøkkel
internal typealias NotifikasjonTopic<T> = KafkaTopic<NokkelInput, T>

internal data class Ident(val ident: String)

internal class Notifikasjoner(
    private val repository: NotifikasjonRepository,
    private val brukervarselTopic: NotifikasjonTopic<String>,
) {
    fun send(kommando: Beskjed) {
        kommando.lagre(repository)
        kommando.send(brukervarselTopic)
    }

    fun send(kommando: Oppgave) {
        kommando.lagre(repository)
        kommando.send(brukervarselTopic)
    }

    fun send(kommando: Done) {
        kommando.lagre(repository)
        kommando.send(brukervarselTopic)
    }
}

internal abstract class NotifikasjonKommando {
    protected abstract val eventId: UUID
    abstract fun getNøkkel(): Nøkkel
    protected abstract fun getMelding(): NotifikasjonMelding<*>

    // TODO: Er nå Any for å funke med ny String-input fra Beskjed og SpecificRecord fra de andre typene
    fun <T : Any> send(topic: NotifikasjonTopic<T>) =
        @Suppress("UNCHECKED_CAST")
        topic.publiser(getNøkkel().somInput(), getMelding().somInput() as T)

    abstract fun lagre(repository: NotifikasjonRepository): Boolean
}
