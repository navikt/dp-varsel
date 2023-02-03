package no.nav.dagpenger.behov.brukernotifikasjon.db

import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import java.net.URL
import java.time.LocalDateTime
import java.util.*

internal object OppgaveObjectMother {

    fun giveMeOppgave(
        ident: Ident = Ident("***********"),
        eventId: UUID = UUID.randomUUID(),
        link: URL = URL("https://dummyOppgave/123"),
        tekst: String = "Dette er en oppgave",
        aktiv: Boolean = true,
        søknadId: UUID = UUID.randomUUID(),
        synligFramTil: LocalDateTime = LocalDateTime.now().plusWeeks(3)
    ) = Oppgave(
        ident = ident,
        eventId = eventId,
        tekst = tekst,
        opprettet = LocalDateTime.now().minusDays(90),
        sikkerhetsnivå = 3,
        eksternVarsling = false,
        link = link,
        søknadId = søknadId,
        deaktiveringstidspunkt = null,
        deaktiveringsgrunn = null,
        synligFramTil = synligFramTil,
        aktiv = aktiv
    )

}
