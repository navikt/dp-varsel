package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import tools.jackson.databind.JsonNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.asUUID
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Notifikasjoner
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import java.net.URL
import java.time.LocalDateTime

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

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val behovId = packet["@behovId"].asUUID()
        val ident = packet["ident"].asText()
        val søknadId = packet["søknad_uuid"].asUUID()

        withLoggingContext(
            "behovId" to behovId.toString(),
            "søknadId" to søknadId.toString()
        ) {
            logger.info { "Løser behov for brukernotifikasjon: oppgave" }

            notifikasjoner.send(
                Oppgave(
                    ident = Ident(ident),
                    eventId = behovId,
                    tekst = packet["tekst"].asText(),
                    opprettet = packet["@opprettet"].asLocalDateTime(),
                    link = packet["link"].asUrl(),
                    søknadId = søknadId,
                    synligFramTil = LocalDateTime.now().plusWeeks(3)
                )
            )
        }
    }
}

internal fun JsonNode.asUrl() = URL(asText())
