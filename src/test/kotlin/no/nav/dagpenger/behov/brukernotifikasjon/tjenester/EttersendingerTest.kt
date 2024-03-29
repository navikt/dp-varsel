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

class EttersendingerTest {

    @Test
    fun `Skal opprette ny oppgave kun hvis det ikke finnes en oppgave for søknaden fra før`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>(relaxed = true)
        val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepo)

        val nyOppgave = giveMeOppgave()
        ettersendinger.opprettOppgave(nyOppgave)

        verify { notifikasjoner.send(any<Oppgave>()) }
    }

    @Test
    fun `Skal ikke opprette ny oppgave hvis det finnes en oppgave for søknaden fra før`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>()
        every { notifikasjonRepo.hentAktiveOppgaver(any(), any()) } returns listOf(giveMeOppgave())
        val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepo)

        val nyOppgave = giveMeOppgave()
        ettersendinger.opprettOppgave(nyOppgave)

        verify(exactly = 0) { notifikasjoner.send(any<Oppgave>()) }
    }

    @Test
    fun `Skal kunne har flere oppgaver, hvis søker har flere innsendte søknader`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>()
        val ident = Ident("***********")
        val oppgave1 = giveMeOppgave(ident = ident)
        val oppgave2 = giveMeOppgave(ident = ident, søknadId = UUID.randomUUID())
        every { notifikasjonRepo.hentAktiveOppgaver(ident, oppgave1.getSnapshot().søknadId) } returns emptyList() andThen listOf(oppgave1)
        every { notifikasjonRepo.hentAktiveOppgaver(ident, oppgave2.getSnapshot().søknadId) } returns emptyList() andThen listOf(oppgave2)
        val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepo)

        ettersendinger.opprettOppgave(oppgave1)
        ettersendinger.opprettOppgave(oppgave2)

        verify(exactly = 2) { notifikasjoner.send(any<Oppgave>()) }
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
        val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepo)

        ettersendinger.markerOppgaveSomUtført(utførtEvent)

        val verifisertParameter = slot<Done>()
        verify { notifikasjoner.send(capture(verifisertParameter)) }

        val sendtKommando = verifisertParameter.captured
        assertEquals(expectedIdent, sendtKommando.getNøkkel().ident)
        assertEquals(expectedEventId, sendtKommando.getNøkkel().eventId)
        assertEquals(Done.Eventtype.OPPGAVE, sendtKommando.getSnapshot().eventtype)
        assertEquals(expectedTidspunkt, sendtKommando.getSnapshot().tidspunkt)
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
        val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepo)

        ettersendinger.markerOppgaveSomUtført(utførtEvent)

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
        val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepo)

        ettersendinger.markerOppgaveSomUtført(utførtEvent)

        verify(exactly = 0) { notifikasjoner.send(any<Done>()) }
    }

    @Test
    fun `Skal deaktivere alle oppgaver for en bestemt bruker`() {
        val ident = Ident("56478965481")
        val grunn = Done.Grunn.VEDTAK_ELLER_AVSLAG
        val deaktivering = Deaktivering(ident, LocalDateTime.now(), grunn)
        val oppgave1 = giveMeOppgave(ident = ident, søknadId = UUID.randomUUID())
        val oppgave2 = giveMeOppgave(ident = ident, søknadId = UUID.randomUUID())

        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>(relaxed = true)
        every { notifikasjonRepo.hentAlleAktiveOppgaver(ident) } returns listOf(oppgave1, oppgave2)
        val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepo)

        ettersendinger.deaktiverAlleOppgaver(deaktivering)

        val sendteKommandoer = mutableListOf<Done>()

        verify(exactly = 2) {
            notifikasjoner.send(capture(sendteKommandoer))
        }
        sendteKommandoer.forEach { kommando ->
            assertEquals(grunn, kommando.getSnapshot().grunn)
        }
    }

    @Test
    fun `Skal ikke gjøre noe hvis bruker ikke har noen oppgaver`() {
        val ident = Ident("56478965481")
        val deaktivering = Deaktivering(ident, LocalDateTime.now(), Done.Grunn.VEDTAK_ELLER_AVSLAG)

        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>(relaxed = true)
        val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepo)

        ettersendinger.deaktiverAlleOppgaver(deaktivering)

        verify(exactly = 0) {
            notifikasjoner.send(any<Done>())
        }
    }

}
