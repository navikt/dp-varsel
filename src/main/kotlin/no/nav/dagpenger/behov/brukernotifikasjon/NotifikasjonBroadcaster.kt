package no.nav.dagpenger.behov.brukernotifikasjon

import mu.KotlinLogging
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import java.time.LocalDateTime
import java.util.*

internal class NotifikasjonBroadcaster(
    private val mottakerkilde: Mottakerkilde,
    private val notifikasjoner: Notifikasjoner
) {

    companion object {
        private val tekst = "Din påbegynte dagpengesøknad må fullføres innen 15.12.22, eller så må det opprettes en ny."
        private val logger = KotlinLogging.logger {}
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }

    fun sendBeskjedTilAlleIdenterISecreten(dryRun: Boolean): Oppsummering {
        val identer: List<Ident> = mottakerkilde.hentMottakere()
        logger.info("Hentet ${identer.size} identer")

        var success = 0
        var feilet = 0
        if (dryRun) {
            logger.info("Dry run, ville ha produsert ${identer.size} beskjeder.")

        } else {
            logger.info("Skal produsere beskjeder til ${identer.size} identer")
            identer.forEach { ident ->
                try {
                    val beskjeden =
                        Beskjed(UUID.randomUUID(), ident, tekst, LocalDateTime.now(), 3, eksternVarsling = true)
                    notifikasjoner.send(beskjeden)
                    sikkerLogger.info("Sendte beskjed til $ident")
                    success++

                } catch (e: Exception) {
                    feilet++
                    sikkerLogger.warn("Klarte ikke å sende beskjeden til $ident")
                }
            }
        }
        val oppsummering = Oppsummering(success, feilet, identer.size)
        logger.info("Oppsummering: $oppsummering")
        return oppsummering
    }

    internal data class Oppsummering(
        val success: Int,
        val feilet: Int,
        val skulleProdusert: Int
    )
}
