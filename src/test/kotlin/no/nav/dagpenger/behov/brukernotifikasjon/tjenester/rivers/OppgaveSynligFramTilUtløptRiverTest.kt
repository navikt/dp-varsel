package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OppgaveSynligFramTilUtløptRiverTest {
    private val ettersendinger = mockk<Ettersendinger>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            OppgaveSynligFramTilUtløptRiver(this, ettersendinger)
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Skal oppdatere vår database i det oppgaver har blitt deaktivert av Min Side fordi synligFramTilTidspunktet er passert`() {
        val expectedEventId = UUID.randomUUID()
        rapid.sendTestMessage(deaktivering(expectedEventId))

        val utløptOppgave = slot<UUID>()

        verify {
            ettersendinger.markerSomUtløpt(capture(utløptOppgave))
        }

        assertEquals(expectedEventId, utløptOppgave.captured)
    }
}

//language=JSON
private fun deaktivering(eventId: UUID) =
    """
    {
      "@event_name": "inaktivert",
      "varselType": "oppgave",
      "eventId": "$eventId",
      "namespace": "teamdagpenger",
      "appnavn": "dp-varsel"
    }
    """.trimIndent()
