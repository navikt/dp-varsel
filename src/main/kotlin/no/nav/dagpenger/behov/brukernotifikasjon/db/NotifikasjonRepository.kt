package no.nav.dagpenger.behov.brukernotifikasjon.db

import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave

internal interface NotifikasjonRepository {
    fun lagre(beskjed: Beskjed): Boolean
    fun lagre(oppgave: Oppgave): Boolean
}
