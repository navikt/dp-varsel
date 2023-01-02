package no.nav.dagpenger.behov.brukernotifikasjon.db

import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import java.util.*

internal interface NotifikasjonRepository {
    fun lagre(beskjed: Beskjed): Boolean
    fun lagre(oppgave: Oppgave): Boolean
    fun lagre(done: Done): Boolean

    fun hentOppgaver(ident: Ident): List<Oppgave>

    fun hentOppgaver(ident: Ident, søknadId: UUID): List<Oppgave>
}
