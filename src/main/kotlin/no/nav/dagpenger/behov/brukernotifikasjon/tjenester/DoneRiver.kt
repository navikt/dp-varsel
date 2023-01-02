package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.helse.rapids_rivers.*
import java.util.*

internal class DoneRiver(
    rapidsConnection: RapidsConnection,
    private val notifikasjoner: Notifikasjoner
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("brukernotifikasjon")) }
            validate { it.requireValue("type", "done") }
            validate {
                it.requireKey(
                    "@behovId",
                    "@opprettet",
                    "ident",
                    "event_id"
                )
            }

        }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val behovId = packet["@behovId"].asUUID()
        val ident = packet["ident"].asText()
        val eventId = packet.eventId()

        withLoggingContext(
            "behovId" to behovId.toString()
        ) {
            logger.info { "Løser behov for brukernotifikasjon: done" }

            notifikasjoner.send(
                Done(
                    eventId = eventId,
                    ident = Ident(ident),
                    deaktiveringstidspunkt = packet["@opprettet"].asLocalDateTime(),
                    eventtype = Done.Eventtype.OPPGAVE
                )
            )
        }
    }
}

internal fun JsonMessage.eventId() = this["event_id"].asText().let { UUID.fromString(it) }
