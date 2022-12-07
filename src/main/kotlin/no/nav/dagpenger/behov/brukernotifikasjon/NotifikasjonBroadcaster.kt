package no.nav.dagpenger.behov.brukernotifikasjon

import mu.KotlinLogging

internal class NotifikasjonBroadcaster(private val mottakerkilde: Mottakerkilde) {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }

    fun sendBeskjedTilAlleIdenterISecreten(dryRun: Boolean): Int {
        val identer: List<Ident> = mottakerkilde.hentMottakere()
        logger.info("Hentet ${identer.size} identer")

        identer.forEachIndexed { index, ident ->
            sikkerLogger.info("Ident $index: $ident")
        }

        if (!dryRun) {
            logger.info("Skal bestille beskjed til ${identer.size} brukere...")
        }
        return identer.size
    }
}
