package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.KafkaTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonMelding
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import java.util.UUID

internal data class Ident(val ident: String)

internal class Notifikasjoner(
    private val repository: NotifikasjonRepository,
    private val brukervarselTopic: KafkaTopic<String, String>,
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
    protected abstract fun getMelding(): NotifikasjonMelding

    fun send(topic: KafkaTopic<String, String>) =
        topic.publiser(getNøkkel().eventId.toString(), getMelding().somInput())

    abstract fun lagre(repository: NotifikasjonRepository): Boolean
}
