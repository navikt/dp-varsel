package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import mu.KotlinLogging
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import java.net.URL
import java.time.LocalDateTime
import java.util.*

internal class NotifikasjonBroadcaster(
    private val mottakerkilde: Mottakerkilde,
    private val notifikasjoner: Notifikasjoner
) {
    companion object {
        private val tekst =
            "Du har en påbegynt søknad om dagpenger, som snart vil bli slettet på grunn av oppdateringer i systemene våre."
        private val logger = KotlinLogging.logger {}
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }

    fun sendBeskjedTilAlleIdenterISecreten(dryRun: Boolean): Oppsummering {
        val identer: List<Ident> = mottakerkilde.hentMottakere()
        logger.info("Hentet ${identer.size} identer")
        val oppsummering = if (dryRun) {
            logger.info("Dry run, ville ha produsert ${identer.size} beskjeder.")
            Oppsummering(0, 0, identer.size)
        } else {
            identer.sendEnBeskjedTilHver()
        }

        logger.info("Oppsummering: $oppsummering")
        return oppsummering
    }

    private fun List<Ident>.sendEnBeskjedTilHver(): Oppsummering {
        logger.info("Skal produsere beskjeder til $size identer")
        var success = 0
        var feilet = 0
        val tidspunkt = LocalDateTime.now()
        forEach { ident ->
            try {
                val beskjeden = Beskjed(
                    UUID.randomUUID(),
                    ident,
                    tekst,
                    tidspunkt,
                    3,
                    eksternVarsling = true,
                    URL("https://www.nav.no/arbeid/dagpenger/mine-dagpenger")
                )
                notifikasjoner.send(beskjeden)
                sikkerLogger.info("Sendte beskjed til $ident")
                success++
            } catch (e: Exception) {
                feilet++
                sikkerLogger.warn("Klarte ikke å sende beskjeden til $ident", e)
            }
        }
        return Oppsummering(success, feilet, size)
    }

    internal data class Oppsummering(
        val success: Int,
        val feilet: Int,
        val skulleProdusert: Int
    )
}
