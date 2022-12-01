package no.nav.dagpenger.behov.brukernotifikasjon.kafka

import mu.KotlinLogging
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.dagpenger.behov.brukernotifikasjon.config
import no.nav.dagpenger.behov.brukernotifikasjon.nais_app_name
import no.nav.dagpenger.behov.brukernotifikasjon.nais_namespace
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal class NotifikasjonTopic<T : SpecificRecord>(
    private val producer: KafkaProducer<NokkelInput, T>,
    private val topic: String
) {
    companion object {
        val logger = KotlinLogging.logger {}
    }

    fun publiser(nøkkel: NokkelInput, notifikasjon: T) {
        producer.send(ProducerRecord(topic, nøkkel, notifikasjon))
    }
}

internal fun nøkkel(eventId: String, ident: String, grupperingsId: String) = NokkelInputBuilder().apply {
    withEventId(eventId)
    withFodselsnummer(ident)
    withGrupperingsId(grupperingsId)
    withAppnavn(config[nais_app_name])
    withNamespace(config[nais_namespace])
}.build()
