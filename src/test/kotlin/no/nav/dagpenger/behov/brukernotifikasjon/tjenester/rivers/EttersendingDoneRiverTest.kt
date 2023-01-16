package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class EttersendingDoneRiverTest {
    private val ettersendinger = mockk<Ettersendinger>()
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
        val expectedDeaktiverteOppgaver = listOf(UUID.randomUUID())
        every { ettersendinger.markerOppgaveSomUtført(any()) } returns expectedDeaktiverteOppgaver
        rapid.sendTestMessage(ettersendelseoppgaveUtførtBehov.toJson())

        verify {
            ettersendinger.markerOppgaveSomUtført(any())
        }
        val løsning = rapid.inspektør.message(0)["@løsning"]
        val deaktiverteOppgaver = løsning["OppgaveOmEttersendingLøst"]["deaktiverteOppgaver"]
        assertEquals(1, deaktiverteOppgaver.size())
        assertEquals(expectedDeaktiverteOppgaver[0].toString(), deaktiverteOppgaver[0].asText())
    }
}

val ettersendelseoppgaveUtførtBehov = JsonMessage.newNeed(
    behov = listOf("OppgaveOmEttersendingLøst"),
    map = mapOf(
        "ident" to "12312312312",
        "søknad_uuid" to UUID.randomUUID()
    )
)
