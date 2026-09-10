package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.asUUID
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.util.UUID

internal class SøknadsbehandlingFerdigRiver(
    rapidsConnection: RapidsConnection,
    private val ettersendinger: Ettersendinger
) : River.PacketListener {
    private val eventnavn = "søknadsbehandling_ferdig"

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", eventnavn) }
            validate {
                it.requireKey(
                    "@opprettet",
                    "ident",
                    "behandlingId",
                    "søknadId",
                    "førteTil"
                )
            }
        }.register(this)
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val ident = Ident(packet["ident"].asText())
        val behandlingId = packet["behandlingId"].asUUID()
        val søknadId = packet["søknadId"].asUUID()
        val førteTil = packet["førteTil"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()

        withLoggingContext(
            "søknadId" to søknadId.toString(),
            "behandlingId" to behandlingId.toString(),
            "førteTil" to førteTil
        ) {
            logger.info { "Mottok ferdigbehandlet søknad" }
            sikkerLogger.info { "Mottok ferdigbehandlet søknad for person ${ident.ident}: ${packet.toJson()}" }
            ettersendinger.søknadsbehandlingFerdig(ident, søknadId, opprettet)
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logger.warn { "En søknadsbehandling_ferdig-melding kunne ikke valideres." }
        sikkerLogger.warn { "En søknadsbehandling_ferdig-melding kunne ikke valideres: ${problems.toExtendedReport()}" }
    }
}
