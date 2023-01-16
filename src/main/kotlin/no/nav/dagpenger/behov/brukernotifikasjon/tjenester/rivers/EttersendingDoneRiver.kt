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

    private val behov = "OppgaveOmEttersendingLøst"

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf(behov)) }
            validate {
                it.requireKey(
                    "@behovId",
                    "@opprettet",
                    "ident",
                    "søknad_uuid"
                )
            }
            validate { it.rejectKey("@løsning") }

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
            logger.info { "Løser behov for brukernotifikasjon: $behov" }

            val doneEvent = EttersendingUtført(
                søknadId = søknadId,
                ident = ident,
                deaktiveringstidspunkt = packet["@opprettet"].asLocalDateTime(),
            )
            val deaktiverteOppgaver = ettersendinger.markerOppgaveSomUtført(doneEvent)

            packet["@løsning"] = mapOf(
                behov to mapOf(
                    "deaktiverteOppgaver" to deaktiverteOppgaver
                )
            )
            context.publish(packet.toJson())
        }
    }
}
