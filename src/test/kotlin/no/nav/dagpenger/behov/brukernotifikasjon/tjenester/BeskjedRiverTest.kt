package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class BeskjedRiverTest {
    private val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            BeskjedRiver(this, notifikasjoner)
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
            notifikasjoner.send(any<Beskjed>())
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
