package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.helse.rapids_rivers.*
import java.net.URL
import java.util.*

internal class EttersendingsoppgaveRiver(
    rapidsConnection: RapidsConnection,
    private val ettersendelseHandler: EttersendelseHandler
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("brukernotifikasjon")) }
            validate { it.requireValue("type", "ettersendingsoppgave") }
            validate {
                it.requireKey(
                    "@behovId",
                    "@opprettet",
                    "ident",
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

    private val ingressForSøknadsdialogen = "https://arbeid.dev.nav.no/dagpenger/dialog/soknad"
    private val oppgavetekst = "Du må ettersende et eller flere vedlegg til din søknad om Dagpenger"

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val behovId = packet["@behovId"].asUUID()
        val ident = packet["ident"].asText()
        val søknadId = packet.søknadUUID()

        withLoggingContext(
            "behovId" to behovId.toString(),
            "søknadId" to søknadId.toString()
        ) {
            logger.info { "Løser behov for brukernotifikasjon: ettersendingsoppgave" }

            ettersendelseHandler.opprettHvisIkkeFinnesFraFør(
                Oppgave(
                    eventId = behovId,
                    ident = Ident(ident),
                    tekst = oppgavetekst,
                    opprettet = packet["@opprettet"].asLocalDateTime(),
                    link = urlTilKvitteringssiden(søknadId),
                    søknadId = søknadId
                )
            )
        }
    }

    private fun urlTilKvitteringssiden(søknadId: UUID) = URL("$ingressForSøknadsdialogen/$søknadId/kvittering")

}
