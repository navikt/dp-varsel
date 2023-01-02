package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.*

class EttersendelseHandlerTest {

    @Test
    fun `Skal opprette ny oppgave kun hvis det ikke finnes en oppgave for søknaden fra før`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>(relaxed = true)
        val eh = EttersendelseHandler(notifikasjoner, notifikasjonRepo)

        val nyOppgave = createOppgave()
        eh.opprettHvisIkkeFinnesFraFør(nyOppgave)

        verify { notifikasjoner.send(any<Oppgave>()) }
    }

    @Test
    fun `Skal ikke opprette ny oppgave kun hvis det finnes en oppgave for søknaden fra før`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val notifikasjonRepo = mockk<NotifikasjonRepository>()
        every { notifikasjonRepo.hentOppgaver(any(), any()) } returns listOf(createOppgave())
        val eh = EttersendelseHandler(notifikasjoner, notifikasjonRepo)

        val nyOppgave = createOppgave()
        eh.opprettHvisIkkeFinnesFraFør(nyOppgave)

        verify(exactly = 0) { notifikasjoner.send(any<Oppgave>()) }
    }

    private fun createOppgave(
        ident: Ident = Ident("12345"),
        eventId: UUID = UUID.randomUUID(),
        link: URL = URL("https://dummyOppgave/123"),
        tekst: String = "Dette er en oppgave for ettersendelse",
        søknadId: UUID = UUID.randomUUID()
    ) = Oppgave(
        ident = ident,
        eventId = eventId,
        link = link,
        tekst = tekst,
        søknadId = søknadId
    )

}
