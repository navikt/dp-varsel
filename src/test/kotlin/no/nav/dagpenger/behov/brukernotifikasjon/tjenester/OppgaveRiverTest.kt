package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*

internal class OppgaveRiverTest {
    private val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            OppgaveRiver(this, notifikasjoner)
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
        rapid.sendTestMessage(oppgaveBehov.toJson())

        verify {
            notifikasjoner.send(any<Oppgave>())
        }
    }
}

val oppgaveBehov = JsonMessage.newNeed(
    behov = listOf("brukernotifikasjon"),
    map = mapOf(
        "type" to "oppgave",
        "ident" to "12312312312",
        "tekst" to "1-2-3 nå kommer en oppgave",
        "link" to "https://url.til.oppgaven/123",
        "søknad_uuid" to UUID.randomUUID()
    )
)
