package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.EttersendingUtført
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertTrue

internal class DokumentInnsendtRiverTest {
    private val ettersendinger = mockk<Ettersendinger>(relaxed = true)
    private val soknadsdialogensUrl = URL("https://soknadsdialogsurl")
    private val brukerdialogUrl = URL("https://brukerdialogurl")
    private val rapid by lazy {
        TestRapid().apply {
            DokumentInnsendtRiver(this, ettersendinger, soknadsdialogensUrl, brukerdialogUrl)
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
        val opprettet = LocalDateTime.of(2026, 8, 16, 10, 11, 12)
        val event = dokumentkravInnsendtEventMedKrav(
            opprettet,
            DokumentKravInnsending("navn1", "skjemakode", "SEND_SENERE")
        )
        rapid.sendTestMessage(event.toJson())

        val opprettetOppgave = slot<Oppgave>()

        verify {
            ettersendinger.opprettOppgave(capture(opprettetOppgave))
        }

        val snapshotAvOpprettetOppgave = opprettetOppgave.captured.getSnapshot()
        assertContains(snapshotAvOpprettetOppgave.link.toString(), søknadId.toString())
        assertTrue { snapshotAvOpprettetOppgave.link.toString().startsWith(soknadsdialogensUrl.toString()) }
        assertEquals(opprettet.toLocalDate().plusDays(4).atTime(14, 0), snapshotAvOpprettetOppgave.eksternVarslingUtsendingstidspunkt)
        val nå = LocalDateTime.now()
        val omTreUkerMinusEtMinutt = nå.plusWeeks(3).minusMinutes(1)
        val omTreUkerPlusEtMinutt = nå.plusWeeks(3).plusMinutes(1)
        assertTrue(snapshotAvOpprettetOppgave.synligFramTil.isAfter(omTreUkerMinusEtMinutt))
        assertTrue(snapshotAvOpprettetOppgave.synligFramTil.isBefore(omTreUkerPlusEtMinutt))
    }


    @Test
    fun `skal publisere oppgave hvis minst et dokumentkrav skal sendes senere fra orkestrator søknad`() {
        val opprettet = LocalDateTime.of(2026, 8, 16, 10, 11, 12)
        val event = dokumentkravInnsendtEventFraOrkestratorMedKrav(
            opprettet,
            DokumentKravInnsending("navn1", "skjemakode", "SEND_SENERE")
        )
        rapid.sendTestMessage(event.toJson())

        val opprettetOppgave = slot<Oppgave>()

        verify {
            ettersendinger.opprettOppgave(capture(opprettetOppgave))
        }

        val snapshotAvOpprettetOppgave = opprettetOppgave.captured.getSnapshot()
        assertContains(snapshotAvOpprettetOppgave.link.toString(), søknadId.toString())
        assertTrue { snapshotAvOpprettetOppgave.link.toString().startsWith(brukerdialogUrl.toString()) }
        assertEquals(
            "Hei! Vi mangler dokumenter fra deg for å kunne behandle søknaden din. Logg inn på Nav for å sende inn dokumentene innen 30. august. Vennlig hilsen Nav",
            snapshotAvOpprettetOppgave.eksternVarslingTekst,
        )
        assertEquals(opprettet.toLocalDate().plusDays(4).atTime(14, 0), snapshotAvOpprettetOppgave.eksternVarslingUtsendingstidspunkt)
        val nå = LocalDateTime.now()
        val omTreUkerMinusEtMinutt = nå.plusWeeks(3).minusMinutes(1)
        val omTreUkerPlusEtMinutt = nå.plusWeeks(3).plusMinutes(1)
        assertTrue(snapshotAvOpprettetOppgave.synligFramTil.isAfter(omTreUkerMinusEtMinutt))
        assertTrue(snapshotAvOpprettetOppgave.synligFramTil.isBefore(omTreUkerPlusEtMinutt))
    }

    @Test
    fun `skal publisere deaktivere oppgave hvis ingen flere av dokumentkravene skal sendes senere`() {
        val opprettet = LocalDateTime.of(2026, 8, 16, 10, 11, 12)
        val eventUtenUteståendeKrav = dokumentkravInnsendtEventMedKrav(
            opprettet,
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

fun dokumentkravInnsendtEventMedKrav(
    opprettet: LocalDateTime = LocalDateTime.now(),
    vararg dokumentKravInnsending: DokumentKravInnsending,
) = JsonMessage.newMessage(
    eventName = "dokumentkrav_innsendt",
    map = mapOf(
        "hendelseId" to UUID.randomUUID(),
        "ident" to "12312312312",
        "søknad_uuid" to søknadId,
        "@opprettet" to opprettet,
        "dokumentkrav" to dokumentKravInnsending.map {
            mapOf(
                "dokumentnavn" to it.dokumentnavn,
                "skjemakode" to it.skjemakode,
                "valg" to it.valg
            )
        }
    )
)

fun dokumentkravInnsendtEventFraOrkestratorMedKrav(
    opprettet: LocalDateTime,
    vararg dokumentKravInnsending: DokumentKravInnsending,
) = JsonMessage.newMessage(
    eventName = "dokumentkrav_innsendt",
    map = mapOf(
        "hendelseId" to UUID.randomUUID(),
        "ident" to "12312312312",
        "søknad_uuid" to søknadId,
        "kilde" to "orkestrator",
        "@opprettet" to opprettet,
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
