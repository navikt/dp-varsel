package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.EttersendingUtført
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.*

internal class AvslagPåMinsteinntektRiver(
    rapidsConnection: RapidsConnection,
    private val ettersendinger: Ettersendinger
) : River.PacketListener {

    private val eventnavn = "prosess_resultat"

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", eventnavn) }
            validate { it.demandValue("versjon_navn", "AvslagPåMinsteinntekt") }
            validate { it.demandValue("resultat", false) }
            validate {
                it.requireKey(
                    "@opprettet",
                    "søknad_uuid",
                    "identer",
                    "resultat",
                    "subsumsjoner",
                    "fakta"
                )
            }
        }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadId = packet.søknadUUID()

        withLoggingContext(
            "søknadId" to søknadId.toString(),
        ) {
            logger.info { "Fant event av typen '$eventnavn' for AvslagPåMinsteinntekt" }

            val ident = packet["identer"].hentAktivIdent()
            val opprettet = packet["@opprettet"].asLocalDateTime()

            logger.info { "Eventuell tilhørende oppgave om ettersending kan deaktiveres" }
            val doneEvent = EttersendingUtført(
                søknadId = søknadId,
                ident = ident,
                deaktiveringstidspunkt = opprettet,
            )
            ettersendinger.markerOppgaveSomUtført(doneEvent)
        }
    }

    private fun JsonNode.hentAktivIdent() =
        filter { it["type"].asText() == "folkeregisterident" && !it["historisk"].asBoolean() }
            .map { Ident(it["id"].asText()) }
            .singleOrNull() ?: throw IllegalArgumentException("Mottokk ingen qyldig id")

}
