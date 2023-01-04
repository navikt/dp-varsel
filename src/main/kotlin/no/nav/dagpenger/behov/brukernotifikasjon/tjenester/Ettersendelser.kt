package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import mu.KotlinLogging
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave

internal class Ettersendelser(
    private val notifikasjoner: Notifikasjoner,
    private val notifikasjonRepository: NotifikasjonRepository
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun opprettHvisIkkeFinnesFraFør(nyOppgave: Oppgave) {
        val eksisterendeOppgaverForSøknadId = eksisterendeOppgaverForSammeSøknadId(nyOppgave)

        if (eksisterendeOppgaverForSøknadId.isEmpty()) {
            notifikasjoner.send(nyOppgave)
            logger.info("Ny oppgave opprettet.")

        } else {
            logger.info("Søknaden har alt en eller flere oppgaver knyttet til seg. Antall ${eksisterendeOppgaverForSøknadId.size}")
        }

    }

    private fun eksisterendeOppgaverForSammeSøknadId(nyOppgave: Oppgave): List<Oppgave> {
        val snapshotAvNyOppgave = nyOppgave.getSnapshot()
        return notifikasjonRepository.hentOppgaver(snapshotAvNyOppgave.ident, snapshotAvNyOppgave.søknadId)
    }

}
