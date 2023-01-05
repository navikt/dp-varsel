package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*

class EttersendingDoneRiverTest {
    private val ettersendinger = mockk<Ettersendinger>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            EttersendingDoneRiver(this, ettersendinger)
        }
    }

    init {
        System.setProperty("brukernotifikasjon.oppgave.topic", "data")
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal publisere brukernotifikasjoner`() {
        rapid.sendTestMessage(ettersendelseoppgaveUtførtBehov.toJson())

        verify {
            ettersendinger.markerOppgaveSomUtført(any())
        }
    }
}

val ettersendelseoppgaveUtførtBehov = JsonMessage.newNeed(
    behov = listOf("brukernotifikasjon"),
    map = mapOf(
        "type" to "ettersending_done",
        "ident" to "12312312312",
        "søknad_uuid" to UUID.randomUUID()
    )
)
