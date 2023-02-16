package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import java.time.LocalDateTime
import java.util.UUID

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
            logger.info { "Det finnes ingen aktive oppgaver for søknaden, dermed er det ikke noe å deaktivere." }
            return
        }

        if (aktiveOppgaverForSøknaden.erFlereEnnEn()) {
            logger.warn { "Det finnes mer enn en aktiv oppgave for denne søknaden. Antall ${aktiveOppgaverForSøknaden.size}. Alle vil bli markert som utført." }
        }
        aktiveOppgaverForSøknaden.forEach { oppgave: Oppgave ->
            val eventId = oppgave.getSnapshot().eventId
            logger.info { "Skal deaktivere oppgaven med eventId=$eventId" }
            withLoggingContext("eventId" to eventId.toString()) {
                notifikasjoner.send(utførtEttersending.somDoneEvent(eventId))
                logger.info { "Oppgaven har blitt deaktivert." }
            }
        }
    }

    private fun List<Oppgave>.erFlereEnnEn() = size > 1

    fun deaktiverAlleOppgaver(deaktivering: Deaktivering) {
        val aktiveOppgaver = notifikasjonRepository.hentAlleAktiveOppgaver(deaktivering.ident)

        if (aktiveOppgaver.isEmpty()) {
            logger.info { "Det finnes ingen aktive oppgaver for brukeren, dermed er det ikke noe å deaktivere." }
            return
        } else {
            logger.info { "Fant ${aktiveOppgaver.size} aktive oppgaver" }
        }

        aktiveOppgaver.forEach { aktivOppgave ->
            val eventId = aktivOppgave.getSnapshot().eventId
            withLoggingContext("eventId" to eventId.toString()) {
                notifikasjoner.send(deaktivering.somDoneEvent(eventId))
                logger.info { "Oppgaven har blitt deaktivert." }
            }
        }
    }

    fun markerSomUtløpt(eventId: UUID) {
        val oppgaven = notifikasjonRepository.hentOppgave(eventId)
        val snapshot = oppgaven.getSnapshot()
        if (snapshot.aktiv) {
            notifikasjonRepository.lagre(
                Done(
                    ident = snapshot.ident,
                    eventId = snapshot.eventId,
                    grunn = Done.Grunn.UTLOPT,
                    eventtype = Done.Eventtype.OPPGAVE,
                    deaktiveringstidspunkt = LocalDateTime.now()
                )
            )
        } else {
            logger.info { "Oppgaven har alt blitt markert som deaktivert" }
        }
    }
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
            grunn = Done.Grunn.FERDIG,
            eventtype = Done.Eventtype.OPPGAVE
        )
    }
}

internal data class Deaktivering(
    val ident: Ident,
    val tidspunkt: LocalDateTime,
    val grunn: Done.Grunn
) {
    fun somDoneEvent(eventId: UUID): Done {
        return Done(
            eventId = eventId,
            ident = ident,
            deaktiveringstidspunkt = tidspunkt,
            grunn = grunn,
            eventtype = Done.Eventtype.OPPGAVE
        )
    }
}
