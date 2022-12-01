package no.nav.dagpenger.behov.brukernotifikasjon.kafka

import mu.KotlinLogging
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

internal open class NotifikasjonTopic<T : SpecificRecord> private constructor(
    private val producer: KafkaProducer<NokkelInput, T>,
    private val topic: String
) {
    companion object {
        val logger = KotlinLogging.logger {}
    }

    fun publiser(nøkkel: NokkelInput, notifikasjon: T) {
        producer.send(ProducerRecord(topic, nøkkel, notifikasjon)) { _, e: Exception ->
            println(e)
        }
    }

    internal class BeskjedTopic(producer: KafkaProducer<NokkelInput, BeskjedInput>, topic: String) :
        NotifikasjonTopic<BeskjedInput>(
            producer,
            topic
        )
}
