package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.mockk
import io.mockk.verify
import io.mockk.clearAllMocks
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

internal class SøknadsbehandlingFerdigRiverTest {
    private val ettersendinger = mockk<Ettersendinger>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            SøknadsbehandlingFerdigRiver(this, ettersendinger)
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
        clearAllMocks()
    }

    @ParameterizedTest(name = "skal behandle førteTil={0}")
    @ValueSource(
        strings = [
            "Innvilgelse",
            "Avslag",
            "Stans",
            "Gjenopptak",
            "Endring",
            "Opphør",
            "FramtidigUkjentVerdi"
        ]
    )
    fun `skal sende alle førteTil-verdier videre til ettersendinger`(førteTil: String) {
        sendFerdigbehandletSøknad(førteTil = førteTil)

        verify(exactly = 1) {
            ettersendinger.søknadsbehandlingFerdig(Ident(ident), søknadId, any())
        }
    }

    @Test
    fun `skal ikke behandle melding med manglende felt`() {
        rapid.sendTestMessage(
            JsonMessage.newMessage(
                eventName = "søknadsbehandling_ferdig",
                map = mapOf(
                    "ident" to ident,
                    "søknadId" to søknadId,
                    "førteTil" to "Innvilgelse"
                )
            ).toJson()
        )

        verify(exactly = 0) {
            ettersendinger.søknadsbehandlingFerdig(any(), any(), any())
        }
    }

    private fun sendFerdigbehandletSøknad(førteTil: String) {
        rapid.sendTestMessage(
            JsonMessage.newMessage(
                eventName = "søknadsbehandling_ferdig",
                map = mapOf(
                    "ident" to ident,
                    "behandlingId" to behandlingId,
                    "søknadId" to søknadId,
                    "førteTil" to førteTil,
                    "@opprettet" to opprettet
                )
            ).toJson()
        )
    }

    private companion object {
        private const val ident = "12312312312"
        private const val opprettet = "2026-09-10T11:47:05"
        private val behandlingId = UUID.randomUUID()
        private val søknadId = UUID.randomUUID()
    }
}
