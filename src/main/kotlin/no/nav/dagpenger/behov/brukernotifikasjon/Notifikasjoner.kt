package no.nav.dagpenger.behov.brukernotifikasjon

import no.nav.dagpenger.behov.brukernotifikasjon.db.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.db.InMemoryNotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.db.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic.BeskjedTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic.Companion.logger

internal class Notifikasjoner(
    private val repository: NotifikasjonRepository,
    private val beskjedTopic: BeskjedTopic
) {
    constructor(beskjedTopic: BeskjedTopic) : this(InMemoryNotifikasjonRepository, beskjedTopic)

    fun send(nøkkel: Nøkkel, beskjed: Beskjed) {
        repository.lagre(nøkkel, beskjed)
        beskjedTopic.publiser(nøkkel.somNøkkel(), beskjed.somMelding())
            .also { logger.info { "Sender ut $beskjed til $nøkkel" } }
    }
}
