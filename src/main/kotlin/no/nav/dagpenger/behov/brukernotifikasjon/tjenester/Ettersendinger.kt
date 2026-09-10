package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.db.NotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

internal class Ettersendinger(
    private val notifikasjoner: Notifikasjoner,
    private val notifikasjonRepository: NotifikasjonRepository
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun opprettOppgave(nyOppgave: Oppgave) {
        val snapshotAvNyOppgave = nyOppgave.getSnapshot()

        if (notifikasjonRepository.harFerdigbehandletSøknad(snapshotAvNyOppgave.ident, snapshotAvNyOppgave.søknadId)) {
            logger.info { "Søknaden er allerede ferdigbehandlet, oppretter ikke ny oppgave." }
            return
        }

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
        val aktiveOppgaver = notifikasjonRepository.hentAktiveOppgaver(
            utførtEttersending.ident,
            utførtEttersending.søknadId
        )
        deaktiverOppgaver(
            aktiveOppgaver = aktiveOppgaver,
            grunn = Done.Grunn.FERDIG,
            tomMelding = "Det finnes ingen aktive oppgaver for søknaden, dermed er det ikke noe å deaktivere."
        ) { eventId, grunn ->
            utførtEttersending.somDoneEvent(eventId, grunn)
        }
    }

    fun søknadsbehandlingFerdig(ident: Ident, søknadId: UUID, tidspunkt: LocalDateTime) {
        // Markøren må være varig; senere ettersending skal ikke gjenåpne oppgaven.
        notifikasjonRepository.lagreFerdigbehandletSøknad(ident, søknadId)
        val aktiveOppgaverForSøknaden = notifikasjonRepository.hentAktiveOppgaver(
            ident,
            søknadId
        )
        deaktiverOppgaver(
            aktiveOppgaver = aktiveOppgaverForSøknaden,
            grunn = Done.Grunn.VEDTAK_ELLER_AVSLAG,
            tomMelding = "Det finnes ingen aktive oppgaver for søknaden, dermed er det ikke noe å deaktivere."
        ) { eventId, grunn ->
            Done(
                eventId = eventId,
                ident = ident,
                deaktiveringstidspunkt = tidspunkt,
                grunn = grunn,
                eventtype = Done.Eventtype.OPPGAVE
            )
        }
    }

    fun deaktiverAlleOppgaver(deaktivering: Deaktivering) {
        val aktiveOppgaver = notifikasjonRepository.hentAlleAktiveOppgaver(deaktivering.ident)
        deaktiverOppgaver(
            aktiveOppgaver = aktiveOppgaver,
            grunn = deaktivering.grunn,
            tomMelding = "Det finnes ingen aktive oppgaver for brukeren, dermed er det ikke noe å deaktivere."
        ) { eventId, _ ->
            deaktivering.somDoneEvent(eventId)
        }
    }

    private fun deaktiverOppgaver(
        aktiveOppgaver: List<Oppgave>,
        grunn: Done.Grunn,
        tomMelding: String,
        somDoneEvent: (UUID, Done.Grunn) -> Done
    ) {
        if (aktiveOppgaver.isEmpty()) {
            logger.info { tomMelding }
            return
        }

        if (aktiveOppgaver.erFlereEnnEn()) {
            logger.warn { "Det finnes mer enn en aktiv oppgave for denne søknaden. Antall ${aktiveOppgaver.size}. Alle vil bli markert som utført." }
        }

        aktiveOppgaver.forEach { aktivOppgave ->
            val eventId = aktivOppgave.getSnapshot().eventId
            withLoggingContext("eventId" to eventId.toString()) {
                logger.info { "Skal deaktivere oppgaven med eventId=$eventId og grunn=$grunn" }
                notifikasjoner.send(somDoneEvent(eventId, grunn))
                logger.info { "Oppgaven har blitt deaktivert." }
            }
        }
    }

    private fun List<Oppgave>.erFlereEnnEn() = size > 1

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
    fun somDoneEvent(eventId: UUID, grunn: Done.Grunn): Done {
        return Done(
            eventId = eventId,
            ident = ident,
            deaktiveringstidspunkt = deaktiveringstidspunkt,
            grunn = grunn,
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
