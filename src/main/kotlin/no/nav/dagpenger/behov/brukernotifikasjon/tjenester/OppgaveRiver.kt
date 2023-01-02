package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.helse.rapids_rivers.*
import java.net.URL
import java.util.*

internal class OppgaveRiver(
    rapidsConnection: RapidsConnection,
    private val notifikasjoner: Notifikasjoner
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("brukernotifikasjon")) }
            validate { it.requireValue("type", "oppgave") }
            validate {
                it.requireKey(
                    "@behovId",
                    "@opprettet",
                    "ident",
                    "tekst",
                    "link",
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

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val behovId = packet["@behovId"].asUUID()
        val ident = packet["ident"].asText()
        val søknadId = packet.søknadUUID()

        withLoggingContext(
            "behovId" to behovId.toString()
        ) {
            logger.info { "Løser behov for brukernotifikasjon: oppgave" }

            notifikasjoner.send(
                Oppgave(
                    eventId = behovId,
                    ident = Ident(ident),
                    tekst = packet["tekst"].asText(),
                    opprettet = packet["@opprettet"].asLocalDateTime(),
                    link = packet["link"].asUrl(),
                    søknadId = søknadId
                )
            )
        }
    }
}

internal fun JsonNode.asUrl() = URL(asText())
internal fun JsonMessage.søknadUUID() = this["søknad_uuid"].asText().let { UUID.fromString(it) }
