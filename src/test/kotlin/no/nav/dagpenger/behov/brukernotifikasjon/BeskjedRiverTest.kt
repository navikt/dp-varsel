package no.nav.dagpenger.behov.brukernotifikasjon

import io.mockk.mockk
import io.mockk.verify
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.BeskjedRiver
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class BeskjedRiverTest {
    private val producer = mockk<KafkaProducer<NokkelInput, BeskjedInput>>(relaxed = true)
    private val notifikasjonTopic = NotifikasjonTopic(producer, "data")
    private val rapid by lazy {
        TestRapid().apply {
            BeskjedRiver(this, notifikasjonTopic)
        }
    }

    init {
        System.setProperty("brukernotifikasjon.beskjed.topic", "data")
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal publisere brukernotifikasjoner`() {
        rapid.sendTestMessage(beskjedBehov.toJson())

        verify {
            producer.send(any())
        }
    }
}

val beskjedBehov = JsonMessage.newNeed(
    behov = listOf("brukernotifikasjon"),
    map = mapOf(
        "type" to "beskjed",
        "ident" to "12312312312",
        "tekst" to "1-2-3 nå kommer en beskjed"
    )
)
