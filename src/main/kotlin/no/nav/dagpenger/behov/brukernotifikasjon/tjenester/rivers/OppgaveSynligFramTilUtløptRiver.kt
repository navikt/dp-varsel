package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

internal class OppgaveSynligFramTilUtløptRiver(
    rapidsConnection: RapidsConnection,
    private val ettersendinger: Ettersendinger
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "inaktivert") }
            validate { it.demandValue("varselType", "oppgave") }
            validate { it.demandValue("namespace", "teamdagpenger") }
            validate { it.demandValue("appnavn", "dp-varsel") }
            validate {
                it.requireKey(
                    "eventId"
                )
            }
        }.register(this)
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = UUID.fromString(packet["eventId"].asText())

        withLoggingContext(
            "eventId" to eventId.toString()
        ) {
            logger.info { "Tidspunktet for synligFramTil har blitt passert, oppgaven skal settes som inaktiv i vår database." }
            ettersendinger.markerSomUtløpt(eventId)
        }
    }
}
