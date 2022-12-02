package no.nav.dagpenger.behov.brukernotifikasjon.db

import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.BeskjedMelding
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.OppgaveMelding

internal interface NotifikasjonRepository {
    fun lagre(nøkkel: Nøkkel, beskjed: BeskjedMelding): Boolean
    fun lagre(nøkkel: Nøkkel, beskjed: OppgaveMelding): Boolean
}
