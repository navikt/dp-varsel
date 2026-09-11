package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers.River
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

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val eventId = UUID.fromString(packet["eventId"].asText())

        withLoggingContext(
            "eventId" to eventId.toString()
        ) {
            logger.info { "Tidspunktet for synligFramTil har blitt passert, oppgaven skal settes som inaktiv i vår database." }
            ettersendinger.markerSomUtløpt(eventId)
        }
    }
}
