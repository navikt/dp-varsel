package no.nav.dagpenger.behov.brukernotifikasjon.kafka

import mu.KotlinLogging
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.dagpenger.behov.brukernotifikasjon.config
import no.nav.dagpenger.behov.brukernotifikasjon.nais_app_name
import no.nav.dagpenger.behov.brukernotifikasjon.nais_namespace
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

internal class NotifikasjonTopic<T : SpecificRecord> constructor(
    private val producer: KafkaProducer<NokkelInput, T>,
    private val topic: String
) {
    companion object {
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }

    fun publiser(nøkkel: Nøkkel, melding: NotifikasjonMelding<T>) {
        producer.send(ProducerRecord(topic, nøkkel.somInput(), melding.somInput()))
            .also { sikkerLogger.info { "Sender ut $melding til $nøkkel" } }
    }
}

internal interface NotifikasjonMelding<T : SpecificRecord> {
    fun somInput(): T
}

internal data class Nøkkel(internal val eventId: UUID, internal val ident: Ident) {
    constructor(ident: Ident) : this(UUID.randomUUID(), ident)

    fun somInput(): NokkelInput = NokkelInputBuilder().apply {
        withEventId(eventId.toString())
        withFodselsnummer(ident.ident)
        withGrupperingsId("deprecated")
        withAppnavn(config[nais_app_name])
        withNamespace(config[nais_namespace])
    }.build()
}
