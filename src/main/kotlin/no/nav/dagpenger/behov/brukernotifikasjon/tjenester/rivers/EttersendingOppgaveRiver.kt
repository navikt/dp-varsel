package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.*
import java.net.URL
import java.util.*

internal class EttersendingOppgaveRiver(
    rapidsConnection: RapidsConnection,
    private val ettersendinger: Ettersendinger,
    private val soknadsdialogensUrl: URL
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("brukernotifikasjon")) }
            validate { it.requireValue("type", "ettersending_oppgave") }
            validate {
                it.requireKey(
                    "@behovId",
                    "@opprettet",
                    "ident",
                    "søknad_uuid"
                )
            }

            validate {
                it.interestedIn(
                    "eksternVarsling"
                )
            }
        }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    private val oppgavetekst = "Vi mangler dokumentasjon for å kunne behandle søknaden din om dagpenger. Ettersend her."

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val behovId = packet["@behovId"].asUUID()
        val ident = packet["ident"].asText()
        val søknadId = packet.søknadUUID()

        withLoggingContext(
            "behovId" to behovId.toString(),
            "søknadId" to søknadId.toString()
        ) {
            logger.info { "Løser behov for brukernotifikasjon: ettersending_oppgave" }

            ettersendinger.opprettOppgave(
                Oppgave(
                    eventId = behovId,
                    ident = Ident(ident),
                    tekst = oppgavetekst,
                    opprettet = packet["@opprettet"].asLocalDateTime(),
                    link = urlTilEttersendingssiden(søknadId),
                    søknadId = søknadId
                )
            )
        }
    }

    private fun urlTilEttersendingssiden(søknadId: UUID) = URL("$soknadsdialogensUrl/soknad/$søknadId/ettersending")

}
