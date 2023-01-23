package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.EttersendingUtført
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class AutoAvslagFraQuizRiverTest {
    private val ettersendinger = mockk<Ettersendinger>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            AutoAvslagFraQuizRiver(this, ettersendinger)
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
    fun `Skal deaktivere oppgave hvis det kommer et automatisk avslag`() {
        val ident = "12345678901"
        val søknadId = UUID.randomUUID()
        val avslagsEvent = automatiskAvslag(søknadId = søknadId, ident = ident)
        rapid.sendTestMessage(avslagsEvent.toJson())

        val utførtevent = slot<EttersendingUtført>()

        verify {
            ettersendinger.markerOppgaveSomUtført(capture(utførtevent))
        }

        assertEquals(ident, utførtevent.captured.ident.ident)
        assertEquals(søknadId, utførtevent.captured.søknadId)
    }

    @Test
    fun `Skal ignorere AvslagPÅMinsteinntekt hvis resultatet er true`() {
        val ident = "12345678901"
        val søknadId = UUID.randomUUID()
        val avslagsEvent = automatiskAvslag(søknadId = søknadId, ident = ident, resultat = true)
        rapid.sendTestMessage(avslagsEvent.toJson())

        val utførtevent = slot<EttersendingUtført>()

        verify(exactly = 0) {
            ettersendinger.markerOppgaveSomUtført(capture(utførtevent))
        }
    }

    @Test
    fun `Skal ignorere alle andre prosesser enn AvslagPåMinsteinntekt`() {
        val uinteressanteProsesser = listOf(
            automatiskAvslag(prosessnavn = "Dagpenger"),
            automatiskAvslag(prosessnavn = "Innsending"),
            automatiskAvslag(prosessnavn = "Paragraf_4_23_alder")
        )
        uinteressanteProsesser.forEach { uinteressantEvent ->
            rapid.sendTestMessage(uinteressantEvent.toJson())
        }

        verify(exactly = 0) {
            ettersendinger.markerOppgaveSomUtført(any())
        }
    }

}

fun automatiskAvslag(
    prosessnavn: String = "AvslagPåMinsteinntekt",
    søknadId: UUID = UUID.randomUUID(),
    ident: String = "12345678901",
    resultat: Boolean = false
) = JsonMessage.newMessage(
    eventName = "prosess_resultat",
    map = mapOf(
        "versjon_navn" to prosessnavn,
        "søknad_uuid" to søknadId,
        "resultat" to resultat,
        "fakta" to emptyList<String>(),
        "subsumsjoner" to emptyList<String>(),
        "identer" to listOf(
            mapOf(
                "id" to ident,
                "type" to "folkeregisterident",
                "historisk" to resultat
            )
        )
    )
)
