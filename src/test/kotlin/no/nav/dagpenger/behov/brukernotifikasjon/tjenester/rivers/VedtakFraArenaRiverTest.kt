package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Deaktivering
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
    fun `Skal deaktivere alle oppgaver som brukeren har, for versjon 1 av formatet fra Arena`() {
        val expectedIdent = Ident("12345678901")
        val expectedTidspunkt = LocalDateTime.now()
        Vedtaktypekode.values().forEach { kode ->
            rapid.sendTestMessage(vedtakJsonV1(expectedIdent, kode, expectedTidspunkt))
        }

        val deaktiveringer = mutableListOf<Deaktivering>()
        verify {
            ettersendinger.deaktiverAlleOppgaver(capture(deaktiveringer))
        }

        deaktiveringer.forEach { deaktivering ->
            assertEquals(expectedIdent, deaktivering.ident)
            assertEquals(expectedTidspunkt, deaktivering.tidspunkt)
        }
    }

    @Test
    fun `Skal deaktivere alle oppgaver som brukeren har, for versjon 2 av formatet fra Arena`() {
        val expectedIdent = Ident("12345678901")
        val expectedTidspunkt = LocalDateTime.now()
        Vedtaktypekode.values().forEach { vedtaktypekode ->
            rapid.sendTestMessage(vedtakJsonV2(expectedIdent, vedtaktypekode, expectedTidspunkt))
        }

        val deaktiveringer = mutableListOf<Deaktivering>()
        verify(exactly = 3) {
            ettersendinger.deaktiverAlleOppgaver(capture(deaktiveringer))
        }

        deaktiveringer.forEach { deaktivering ->
            assertEquals(expectedIdent, deaktivering.ident)
            assertEquals(expectedTidspunkt, deaktivering.tidspunkt)
        }
    }

    @Test
    fun `Skal kun behandle vedtak som har kommet etter at vi begyte å produsere oppgaver om ettersending`() {
        val tidspunktFørGrensen = LocalDateTime.of(2023, 1, 20, 9, 29)
        val vedtaktypekode = Vedtaktypekode.ORDINÆR
        val ident = Ident("12345678901")
        val forGammeltVedtakVersjon1 = vedtakJsonV1(ident, vedtaktypekode, tidspunktFørGrensen)
        val forGammeltVedtakVersjon2 = vedtakJsonV2(ident, vedtaktypekode, tidspunktFørGrensen)
        rapid.sendTestMessage(forGammeltVedtakVersjon1)
        rapid.sendTestMessage(forGammeltVedtakVersjon2)

        verify(exactly = 0) {
            ettersendinger.deaktiverAlleOppgaver(any())
        }
    }

    private enum class Vedtaktypekode(val kode: String) {
        ENDRING("E"),
        GJENNOPPTAK("G"),
        ORDINÆR("O")
    }

    //language=JSON
    private fun vedtakJsonV1(
        ident: Ident,
        vedtaktypekode: Vedtaktypekode,
        opprettet: LocalDateTime = LocalDateTime.now().minusHours(1)
    ) = """{
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
        "VEDTAKTYPEKODE": "${vedtaktypekode.kode}",
        "UTFALLKODE": "JA",
        "RETTIGHETKODE": "DAGO",
        "PERSON_ID": 4124685,
        "FRA_DATO": "2018-03-05 00:00:00",
        "TIL_DATO": null
      },
      "@opprettet": "$opprettet"
    }
    """.trimIndent()

    //language=JSON
    private fun vedtakJsonV2(
        ident: Ident,
        vedtaktypekode: Vedtaktypekode,
        opprettet: LocalDateTime = LocalDateTime.now().minusHours(2)
    ) = """{
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
            "VEDTAKTYPEKODE": "${vedtaktypekode.kode}",
            "UTFALLKODE": "JA",
            "RETTIGHETKODE": "DAGO",
            "PERSON_ID": 4124685,
            "FRA_DATO": "2018-03-05 00:00:00",
            "TIL_DATO": null
        },
        "@opprettet": "$opprettet"
    }
""".trimIndent()

}
