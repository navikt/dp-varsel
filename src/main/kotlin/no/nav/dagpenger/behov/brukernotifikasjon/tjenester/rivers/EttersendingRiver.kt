package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.EttersendingUtført
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.*
import java.net.URL
import java.time.LocalDateTime
import java.util.*

internal class EttersendingRiver(
    rapidsConnection: RapidsConnection,
    private val ettersendinger: Ettersendinger,
    private val soknadsdialogensUrl: URL
) : River.PacketListener {

    private val eventnavn = "dokumentkrav_innsendt"

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", eventnavn) }
            validate {
                it.requireKey(
                    "@opprettet",
                    "hendelseId",
                    "ident",
                    "søknad_uuid",
                    "dokumentkrav"
                )
            }
        }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    private val oppgavetekst = "Vi mangler dokumentasjon for å kunne behandle søknaden din om dagpenger. Ettersend her."

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadId = packet.søknadUUID()
        val hendelseId = packet["hendelseId"].asUUID()

        withLoggingContext(
            "søknadId" to søknadId.toString(),
            "hendelseId" to hendelseId.toString()
        ) {
            logger.info { "Fant event av typen '$eventnavn', sjekker om oppgave skal opprettes eller deaktiveres" }

            val ident = Ident(packet["ident"].asText())
            val kravene = Dokumentkrav(packet["dokumentkrav"])
            val opprettet = packet["@opprettet"].asLocalDateTime()

            if (kravene.venterPåEttersendinger()) {
                logger.info { "Fant kandidat for å opprette oppgave om ettersending" }
                val nyOppgave = lagOppgave(hendelseId, ident, opprettet, søknadId)
//                ettersendinger.opprettOppgave(nyOppgave)

            } else {
                logger.info { "Fant kandidat for å deaktivere oppgave om ettersending" }
                val doneEvent = lagDoneEvent(søknadId, ident, opprettet)
                //ettersendinger.markerOppgaveSomUtført(doneEvent)
            }

        }
    }

    private fun lagOppgave(
        hendelseId: UUID,
        ident: Ident,
        opprettet: LocalDateTime,
        søknadId: UUID
    ) = Oppgave(
        eventId = hendelseId,
        ident = ident,
        tekst = oppgavetekst,
        opprettet = opprettet,
        link = urlTilEttersendingssiden(søknadId),
        søknadId = søknadId
    )

    private fun urlTilEttersendingssiden(søknadId: UUID) = URL("$soknadsdialogensUrl/soknad/$søknadId/ettersending")

    private fun lagDoneEvent(
        søknadId: UUID,
        ident: Ident,
        opprettet: LocalDateTime
    ) = EttersendingUtført(
        søknadId = søknadId,
        ident = ident,
        deaktiveringstidspunkt = opprettet,
    )

}

class Dokumentkrav(private val dokumentkravene: JsonNode) {
    fun venterPåEttersendinger(): Boolean = dokumentkravene.filter { krav ->
        krav["valg"].asText().equals("SEND_SENERE", ignoreCase = true)
    }.isNotEmpty()
}
