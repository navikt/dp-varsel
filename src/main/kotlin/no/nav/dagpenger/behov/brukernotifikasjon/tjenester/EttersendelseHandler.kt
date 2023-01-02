package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import mu.KotlinLogging
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave

internal class EttersendelseHandler(
    private val notifikasjoner: Notifikasjoner,
    private val notifikasjonRepository: NotifikasjonRepository
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun opprettHvisIkkeFinnesFraFør(nyOppgave: Oppgave) {
        val snapshotAvNyOppgave = nyOppgave.getSnapshot()
        val oppgaver = notifikasjonRepository.hentOppgaver(snapshotAvNyOppgave.ident, snapshotAvNyOppgave.søknadId)

        if(oppgaver.isEmpty()) {
            notifikasjoner.send(nyOppgave)
            logger.info("Ny oppgave opprettet.")

        } else {
            logger.info("Søknaden har alt en eller flere oppgaver knyttet til seg. Antall ${oppgaver.size}")
        }

    }

}
