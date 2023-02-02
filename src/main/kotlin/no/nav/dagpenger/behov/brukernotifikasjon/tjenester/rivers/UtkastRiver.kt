package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.config
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Topic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.asUUID
import no.nav.dagpenger.behov.brukernotifikasjon.soknadsdialogens_url
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.tms.utkast.builder.UtkastJsonBuilder
import java.net.URI

internal typealias UtkastTopic = Topic<String, String>

internal class UtkastRiver(
    rapidsConnection: RapidsConnection,
    private val utkastTopic: UtkastTopic
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "søknad_endret_tilstand") }
            validate { it.requireKey("søknad_uuid", "ident", "gjeldendeTilstand") }
            validate { it.interestedIn("prosessnavn") }
        }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val tilstand = packet["gjeldendeTilstand"].asText()

        withLoggingContext(
            "søknad_uuid" to søknadId.toString(),
            "tilstand" to tilstand
        ) {
            val utkast = SøknadEndretTilstand(packet)
            if (!utkast.skalPubliseres()) {
                logger.info { "Denne endringen skal ikke til utkast" }
                return
            }

            logger.info { "Informerer tms-utkast om endringer" }
            utkastTopic.publiser(
                utkast.nøkkel,
                utkast.utkastJson
            )
        }
    }
}

class SøknadEndretTilstand(packet: JsonMessage) {
    private val søknadId = packet["søknad_uuid"].asUUID()
    private val ident = packet["ident"].asText()
    private val tittel = "Søknad om dagpenger"
    private val link = søknadUrl.resolve("${søknadUrl.path}/soknad/$søknadId")
    private val tilstand = packet["gjeldendeTilstand"].asText()
    private val prosessnavn = packet["prosessnavn"].asText()
    val nøkkel get() = søknadId.toString()

    fun skalPubliseres() = tilstand != "Påbegynt" || prosessnavn == "Dagpenger"

    private fun jsonBuilder() = UtkastJsonBuilder().apply {
        withUtkastId(søknadId.toString())
        withIdent(ident)
        withTittel(tittel)
        withLink(link.toASCIIString())
        withMetrics("dagpenger", søknadId.toString())
    }

    val utkastJson
        get() = when (tilstand) {
            "Påbegynt" -> jsonBuilder().create()
            "Innsendt" -> jsonBuilder().delete()
            "Slettet" -> jsonBuilder().delete()
            else -> throw IllegalArgumentException("Ukjent tilstand på søknad")
        }

    companion object {
        private val søknadUrl: URI
            get() = config[soknadsdialogens_url].also {
                require(it.isAbsolute) { "URL til søknad må være absolutt" }
            }
    }
}
