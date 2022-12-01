package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.nøkkel
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.util.UUID

internal class BeskjedRiver(
    rapidsConnection: RapidsConnection,
    private val beskjedTopic: NotifikasjonTopic<BeskjedInput>
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("brukernotifikasjon")) }
            validate { it.requireValue("type", "beskjed") }
            validate {
                it.requireKey(
                    "@behovId",
                    "@opprettet",
                    "ident",
                    "tekst"
                )
            }

            validate {
                it.interestedIn(
                    "link"
                )
            }
        }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val behovId = packet["@behovId"].asUUID()
        val ident = packet["ident"].asText()

        withLoggingContext(
            "behovId" to behovId.toString()
        ) {
            logger.info { "Løser behov for brukernotifikasjon" }
            val notifikasjon = BeskjedInputBuilder().apply {
                withTekst(packet["tekst"].asText())
                withTidspunkt(packet["@opprettet"].asLocalDateTime())
                withSikkerhetsnivaa(3)
            }.build()
            val nøkkel = nøkkel(behovId.toString(), ident, "yo")
            beskjedTopic.publiser(nøkkel, notifikasjon).also {
                logger.info { "Sender ut $notifikasjon til $nøkkel" }
            }
        }
    }
}

private fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
