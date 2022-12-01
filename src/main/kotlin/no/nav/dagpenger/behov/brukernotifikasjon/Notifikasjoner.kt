package no.nav.dagpenger.behov.brukernotifikasjon

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.dagpenger.behov.brukernotifikasjon.db.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.db.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic.Companion.logger

internal class Notifikasjoner(
    private val beskjedTopic: NotifikasjonTopic<BeskjedInput>
) {
    fun send(nøkkel: Nøkkel, beskjed: Beskjed) {
        beskjedTopic.publiser(nøkkel.somNøkkel(), beskjed.somMelding())
            .also { logger.info { "Sender ut $beskjed til $nøkkel" } }
    }
}
