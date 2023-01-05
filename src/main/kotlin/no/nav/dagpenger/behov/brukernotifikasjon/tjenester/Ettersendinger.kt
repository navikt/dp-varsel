package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import java.time.LocalDateTime
import java.util.*

internal class Ettersendinger(
    private val notifikasjoner: Notifikasjoner,
    private val notifikasjonRepository: NotifikasjonRepository
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun opprettOppgave(nyOppgave: Oppgave) {
        val aktiveOppgaverForSøknadId = eksisterendeAktiveOppgaverForSammeSøknadId(nyOppgave)

        if (aktiveOppgaverForSøknadId.isEmpty()) {
            notifikasjoner.send(nyOppgave)
            logger.info { "Ny oppgave opprettet." }

        } else {
            logger.info { "Søknaden har alt en eller flere aktive oppgaver knyttet til seg. Antall: ${aktiveOppgaverForSøknadId.size}" }
        }

    }

    private fun eksisterendeAktiveOppgaverForSammeSøknadId(nyOppgave: Oppgave): List<Oppgave> {
        val snapshotAvNyOppgave = nyOppgave.getSnapshot()
        return notifikasjonRepository.hentAktiveOppgaver(snapshotAvNyOppgave.ident, snapshotAvNyOppgave.søknadId)
    }

    fun markerOppgaveSomUtført(utførtEttersending: EttersendingUtført) {
        val aktiveOppgaverForSøknaden = notifikasjonRepository.hentAktiveOppgaver(
            utførtEttersending.ident,
            utførtEttersending.søknadId
        )
        if (aktiveOppgaverForSøknaden.isEmpty()) {
            logger.warn { "Det finnes ingen aktive oppgaver for søknadId=${utførtEttersending.søknadId}" }
            return
        }

        if (aktiveOppgaverForSøknaden.erFlereEnnEn()) {
            logger.info { "Det finnes mer enn en aktiv oppgave for denne søknaden. Antall ${aktiveOppgaverForSøknaden.size}. Alle vil bli markert som utført." }
        }
        aktiveOppgaverForSøknaden.forEach { oppgave: Oppgave ->
            val eventId = oppgave.getSnapshot().eventId
            logger.info { "Skal deaktivere oppgaven med eventId=$eventId" }
            withLoggingContext("eventId" to eventId.toString()) {
                notifikasjoner.send(utførtEttersending.somDoneEvent(eventId))
            }
        }
    }

    private fun List<Oppgave>.erFlereEnnEn() = size > 1

}

internal data class EttersendingUtført(
    val ident: Ident,
    val søknadId: UUID,
    private val deaktiveringstidspunkt: LocalDateTime
) {
    fun somDoneEvent(eventId: UUID): Done {
        return Done(
            eventId = eventId,
            ident = ident,
            deaktiveringstidspunkt = deaktiveringstidspunkt,
            eventtype = Done.Eventtype.OPPGAVE
        )
    }
}
