package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.db.OppgaveObjectMother.giveMeOppgave
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import org.junit.jupiter.api.Test

class EttersendelseHandlerTest {

    @Test
    fun `Skal opprette ny oppgave kun hvis det ikke finnes en oppgave for søknaden fra før`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>(relaxed = true)
        val eh = EttersendelseHandler(notifikasjoner, notifikasjonRepo)

        val nyOppgave = giveMeOppgave()
        eh.opprettHvisIkkeFinnesFraFør(nyOppgave)

        verify { notifikasjoner.send(any<Oppgave>()) }
    }

    @Test
    fun `Skal ikke opprette ny oppgave kun hvis det finnes en oppgave for søknaden fra før`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>()
        every { notifikasjonRepo.hentOppgaver(any(), any()) } returns listOf(giveMeOppgave())
        val eh = EttersendelseHandler(notifikasjoner, notifikasjonRepo)

        val nyOppgave = giveMeOppgave()
        eh.opprettHvisIkkeFinnesFraFør(nyOppgave)

        verify(exactly = 0) { notifikasjoner.send(any<Oppgave>()) }
    }

}
