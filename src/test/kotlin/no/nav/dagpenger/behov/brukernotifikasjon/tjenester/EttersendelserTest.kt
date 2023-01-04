package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.db.OppgaveObjectMother.giveMeOppgave
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class EttersendelserTest {

    @Test
    fun `Skal opprette ny oppgave kun hvis det ikke finnes en oppgave for søknaden fra før`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>(relaxed = true)
        val ettersendelser = Ettersendelser(notifikasjoner, notifikasjonRepo)

        val nyOppgave = giveMeOppgave()
        ettersendelser.sendOppgaveHvisIkkeFinnesFraFør(nyOppgave)

        verify { notifikasjoner.send(any<Oppgave>()) }
    }

    @Test
    fun `Skal ikke opprette ny oppgave kun hvis det finnes en oppgave for søknaden fra før`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>()
        every { notifikasjonRepo.hentAktiveOppgaver(any(), any()) } returns listOf(giveMeOppgave())
        val ettersendelser = Ettersendelser(notifikasjoner, notifikasjonRepo)

        val nyOppgave = giveMeOppgave()
        ettersendelser.sendOppgaveHvisIkkeFinnesFraFør(nyOppgave)

        verify(exactly = 0) { notifikasjoner.send(any<Oppgave>()) }
    }

    @Test
    fun `Skal deaktivere en utført oppgave`() {
        val expectedSøknadId = UUID.randomUUID()
        val expectedIdent = Ident("56478965481")
        val expectedEventId = UUID.randomUUID()
        val expectedTidspunkt = LocalDateTime.now()
        val oppgave = giveMeOppgave(ident = expectedIdent, søknadId = expectedSøknadId, eventId = expectedEventId)
        val utførtEvent = EttersendingUtført(expectedIdent, expectedSøknadId, expectedTidspunkt)

        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>()
        every { notifikasjonRepo.hentAktiveOppgaver(any(), any()) } returns listOf(oppgave)
        val ettersendelser = Ettersendelser(notifikasjoner, notifikasjonRepo)

        ettersendelser.merkerOppgaveSomUtført(utførtEvent)

        val verifisertParameter = slot<Done>()
        verify { notifikasjoner.send(capture(verifisertParameter)) }

        val sendtKommando = verifisertParameter.captured
        assertEquals(expectedIdent, sendtKommando.getNøkkel().ident)
        assertEquals(expectedEventId, sendtKommando.getNøkkel().eventId)
        assertEquals(Done.Eventtype.OPPGAVE, sendtKommando.getSnapshot().eventtype)
        assertEquals(expectedTidspunkt, sendtKommando.getSnapshot().deaktiveringstidspunkt)
    }

    @Test
    fun `Skal deaktivere alle oppgaver hvis det finnes flere for en søknad`() {
        val søknadId = UUID.randomUUID()
        val ident = Ident("56478965481")
        val oppgave1 = giveMeOppgave(ident = ident, søknadId = søknadId)
        val oppgave2 = giveMeOppgave(ident = ident, søknadId = søknadId)
        val utførtEvent = EttersendingUtført(ident, søknadId, LocalDateTime.now())

        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>(relaxed = true)
        every { notifikasjonRepo.hentAktiveOppgaver(any(), any()) } returns listOf(oppgave1, oppgave2)
        val ettersendelser = Ettersendelser(notifikasjoner, notifikasjonRepo)

        ettersendelser.merkerOppgaveSomUtført(utførtEvent)

        verify { notifikasjoner.send(any<Done>()) }
    }

    @Test
    fun `Skal ikke gjøre noe hvis det ikke finnes en aktiv ettersendingsoppgave for søknaden`() {
        val søknadId = UUID.randomUUID()
        val ident = Ident("56478965481")
        val utførtEvent = EttersendingUtført(ident, søknadId, LocalDateTime.now())

        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>()
        every { notifikasjonRepo.hentAktiveOppgaver(any(), any()) } returns emptyList()
        val ettersendelser = Ettersendelser(notifikasjoner, notifikasjonRepo)

        ettersendelser.merkerOppgaveSomUtført(utførtEvent)

        verify(exactly = 0) { notifikasjoner.send(any<Done>()) }
    }

}
