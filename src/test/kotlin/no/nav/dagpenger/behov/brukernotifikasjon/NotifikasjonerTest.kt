package no.nav.dagpenger.behov.brukernotifikasjon

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.db.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.db.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic.BeskjedTopic
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import java.util.Properties

internal class NotifikasjonerTest {
    private val beskjedTopic: BeskjedTopic = mockk(relaxed = true)
    private val notifikasjoner = Notifikasjoner(
        repository = mockk(relaxed = true),
        beskjedTopic = beskjedTopic
    )

    @Test
    fun test() {
        notifikasjoner.send(
            Nøkkel("12312312311"),
            Beskjed("1-2-3 nå kommer en beskjed")
        )

        verify {
            beskjedTopic.publiser(any(), any())
        }
    }
}
