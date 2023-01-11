package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.EttersendingUtført
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.*

internal class EttersendingDoneRiver(
    rapidsConnection: RapidsConnection,
    private val ettersendinger: Ettersendinger
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("OppgaveOmEttersendingLøst")) }
            validate {
                it.requireKey(
                    "@behovId",
                    "@opprettet",
                    "ident",
                    "søknad_uuid"
                )
            }

        }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val behovId = packet["@behovId"].asUUID()
        val ident = Ident(packet["ident"].asText())
        val søknadId = packet.søknadUUID()

        withLoggingContext(
            "behovId" to behovId.toString(),
            "søknadId" to søknadId.toString()
        ) {
            logger.info { "Løser behov for brukernotifikasjon: OppgaveOmEttersendingLøst" }
            ettersendinger.markerOppgaveSomUtført(
                EttersendingUtført(
                    søknadId = søknadId,
                    ident = ident,
                    deaktiveringstidspunkt = packet["@opprettet"].asLocalDateTime(),
                )
            )
        }
    }
}
