package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.EttersendingUtført
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertContains
import kotlin.test.assertTrue

internal class DokumentInnsendtRiverTest {
    private val ettersendinger = mockk<Ettersendinger>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            DokumentInnsendtRiver(this, ettersendinger, URL("https://dummyUrl"))
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
    fun `skal publisere oppgave hvis minst et dokumentkrav skal sendes senere`() {
        val event = dokumentkravInnsendtEventMedKrav(
            DokumentKravInnsending("navn1", "skjemakode", "SEND_SENERE")
        )
        rapid.sendTestMessage(event.toJson())

        val opprettetOppgave = slot<Oppgave>()

        verify {
            ettersendinger.opprettOppgave(capture(opprettetOppgave))
        }

        val snapshotAvOpprettetOppgave = opprettetOppgave.captured.getSnapshot()
        assertContains(snapshotAvOpprettetOppgave.link.toString(), søknadId.toString())
        val nå = LocalDateTime.now()
        val omTreUkerMinusEtMinutt = nå.plusWeeks(3).minusMinutes(1)
        val omTreUkerPlusEtMinutt = nå.plusWeeks(3).plusMinutes(1)
        assertTrue(snapshotAvOpprettetOppgave.synligFramTil.isAfter(omTreUkerMinusEtMinutt))
        assertTrue(snapshotAvOpprettetOppgave.synligFramTil.isBefore(omTreUkerPlusEtMinutt))
    }

    @Test
    fun `skal publisere deaktivere oppgave hvis ingen flere av dokumentkravene skal sendes senere`() {
        val eventUtenUteståendeKrav = dokumentkravInnsendtEventMedKrav(
            DokumentKravInnsending("navn1", "skjemakode", "SEND_NÅ"),
            DokumentKravInnsending("navn2", "skjemakode", "SENDER_IKKE")
        )
        rapid.sendTestMessage(eventUtenUteståendeKrav.toJson())

        val ettersendingUtført = slot<EttersendingUtført>()

        verify {
            ettersendinger.markerOppgaveSomUtført(capture(ettersendingUtført))
        }
    }

}

private val søknadId = UUID.randomUUID()

fun dokumentkravInnsendtEventMedKrav(vararg dokumentKravInnsending: DokumentKravInnsending) = JsonMessage.newMessage(
    eventName = "dokumentkrav_innsendt",
    map = mapOf(
        "hendelseId" to UUID.randomUUID(),
        "ident" to "12312312312",
        "søknad_uuid" to søknadId,
        "dokumentkrav" to dokumentKravInnsending.map {
            mapOf(
                "dokumentnavn" to it.dokumentnavn,
                "skjemakode" to it.skjemakode,
                "valg" to it.valg
            )
        }
    )
)

data class DokumentKravInnsending(
    internal val dokumentnavn: String,
    internal val skjemakode: String,
    internal val valg: String
)
