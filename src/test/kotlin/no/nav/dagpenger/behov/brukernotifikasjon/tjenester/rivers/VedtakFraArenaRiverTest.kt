package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VedtakFraArenaRiverTest {

    private val ettersendinger = mockk<Ettersendinger>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            VedtakFraArenaRiver(this, ettersendinger)
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Skal deaktivere alle oppgave som brukeren har, for versjon 1 av formatet fra Arena`() {
        val ident = Ident("12345678901")
        Vedtaktypekode.values().forEach { kode ->
            rapid.sendTestMessage(vedtakJsonV1(ident, kode))
        }

        val capturedIdents = mutableListOf<Ident>()

        verify {
            ettersendinger.markerAlleOppgaverSomUtført(capture(capturedIdents))
        }

        capturedIdents.forEach{ capturedIdent ->
            assertEquals(ident, capturedIdent)
        }
    }

    @Test
    fun `Skal deaktivere alle oppgave som brukeren har, for versjon 2 av formatet fra Arena`() {
        val ident = Ident("12345678901")
        Vedtaktypekode.values().forEach { vedtaktypekode ->
            rapid.sendTestMessage(vedtakJsonV2(ident, vedtaktypekode))
        }

        val capturedIdents = mutableListOf<Ident>()

        verify(exactly = 3) {
            ettersendinger.markerAlleOppgaverSomUtført(capture(capturedIdents))
        }

        capturedIdents.forEach { capturedIdent ->
            assertEquals(ident, capturedIdent)
        }
    }

    private enum class Vedtaktypekode {
        E, G, O
    }

    //language=JSON
    private fun vedtakJsonV1(ident: Ident, vedtaktypekode: Vedtaktypekode) = """{
      "table": "SIAMO.VEDTAK",
      "op_type": "I",
      "op_ts": "2020-04-07 14:31:08.840468",
      "current_ts": "2020-04-07T14:53:03.656001",
      "pos": "00000000000000013022",
      "tokens": {
        "FODSELSNR": "${ident.ident}"
      },
      "after": {
        "VEDTAK_ID": 29501880,
        "SAK_ID": 123,
        "VEDTAKSTATUSKODE": "IVERK",
        "VEDTAKTYPEKODE": "${vedtaktypekode.name}",
        "UTFALLKODE": "JA",
        "RETTIGHETKODE": "DAGO",
        "PERSON_ID": 4124685,
        "FRA_DATO": "2018-03-05 00:00:00",
        "TIL_DATO": null
      }
    }
    """.trimIndent()

    //language=JSON
    private fun vedtakJsonV2(ident: Ident, vedtaktypekode: Vedtaktypekode) = """{
        "table": "SIAMO.VEDTAK",
        "op_type": "I",
        "op_ts": "2021-11-12 08:31:33.092337",
        "current_ts": "2021-11-12 08:57:55.082000",
        "pos": "00000000000000010892",
        "FODSELSNR": "${ident.ident}",
        "after": {
            "VEDTAK_ID": 29501880,
            "SAK_ID": 123,
            "VEDTAKSTATUSKODE": "IVERK",
            "VEDTAKTYPEKODE": "${vedtaktypekode.name}",
            "UTFALLKODE": "JA",
            "RETTIGHETKODE": "DAGO",
            "PERSON_ID": 4124685,
            "FRA_DATO": "2018-03-05 00:00:00",
            "TIL_DATO": null
        }
    }
""".trimIndent()

}
