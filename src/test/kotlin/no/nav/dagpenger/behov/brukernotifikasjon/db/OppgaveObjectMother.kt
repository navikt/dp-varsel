package no.nav.dagpenger.behov.brukernotifikasjon.db

import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import java.net.URL
import java.util.*

internal object OppgaveObjectMother {

    fun giveMeOppgave(
        ident: Ident = Ident("12345678901"),
        eventId: UUID = UUID.randomUUID(),
        link: URL = URL("https://dummyOppgave/123"),
        tekst: String = "Dette er en oppgave",
        søknadId: UUID = UUID.randomUUID()
    ) = Oppgave(
        ident = ident,
        eventId = eventId,
        link = link,
        tekst = tekst,
        søknadId = søknadId
    )

}
