package no.nav.dagpenger.behov.brukernotifikasjon.kafka

import mu.KotlinLogging
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
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
