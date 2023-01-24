package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.helse.rapids_rivers.*

internal class VedtakFraArenaRiver(
    rapidsConnection: RapidsConnection,
    private val ettersendinger: Ettersendinger
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("table", "SIAMO.VEDTAK") }
            validate {
                it.requireKey(
                    "op_ts",
                    "after.VEDTAK_ID",
                    "after.SAK_ID",
                    "after.FRA_DATO"
                )
            }
            validate { it.requireAny("after.VEDTAKTYPEKODE", listOf("O", "G", "E")) }
            validate { it.requireAny("after.UTFALLKODE", listOf("JA", "NEI")) }
            validate { it.interestedIn("after", "tokens") }
            validate { it.interestedIn("after.TIL_DATO") }
            validate { it.interestedIn("tokens.FODSELSNR") }
            validate { it.interestedIn("FODSELSNR") }
        }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val ident = Ident(packet.fødselsnummer())
        val vedtakId = packet["after"]["VEDTAK_ID"].asText()
        val sakId = packet["after"]["SAK_ID"].asText()

        withLoggingContext(
            "fagsakId" to sakId,
            "vedtakId" to vedtakId
        ) {
            logger.info { "Mottok nytt vedtak" }

            ettersendinger.markerAlleOppgaverSomUtført(ident)
        }
    }
}

internal fun JsonMessage.fødselsnummer(): String =
    if (this["tokens"].isMissingOrNull()) this["FODSELSNR"].asText() else this["tokens"]["FODSELSNR"].asText()
