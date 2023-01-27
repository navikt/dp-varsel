package no.nav.dagpenger.behov.brukernotifikasjon.kafka

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.checkerframework.checker.units.qual.K
import java.util.UUID

internal interface Topic<K, V> {
    fun publiser(melding: V) {}
    fun publiser(nøkkel: K, melding: V) {}
}

internal class KafkaTopic<K, V>(
    private val producer: KafkaProducer<K, V>,
    private val topic: String
) : Topic<K, V> {
    companion object {
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }

    override fun publiser(nøkkel: K, melding: V) {
        producer.send(ProducerRecord(topic, nøkkel, melding))
            .also { sikkerLogger.info { "Sender ut $melding til $nøkkel" } }
    }
}

internal fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
