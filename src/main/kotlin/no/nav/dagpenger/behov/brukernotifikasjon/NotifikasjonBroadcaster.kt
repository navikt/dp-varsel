package no.nav.dagpenger.behov.brukernotifikasjon

import mu.KotlinLogging
import java.io.File

class NotifikasjonBroadcaster(private val pathToSecret: String = "/var/run/secrets/brukernotifikasjon-broadcast-beskjed/beskjedBroadcastReceivers.txt") {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun sendBeskjedTilAlleIdenterISecreten(dryRun: Boolean) {
        println("dry run: $dryRun")
        val identer: List<Ident> = lesInnIdenterFraSecret()
        logger.info("Fant ${identer.size} identer")
        identer.forEachIndexed { index, ident ->
            logger.info("Ident $index: $ident")
        }
        if(!dryRun) {
            logger.info("Skal bestille beskjed til ${identer.size} brukere...")
        }
    }

    private fun lesInnIdenterFraSecret() = File(pathToSecret)
        .bufferedReader()
        .readLines()
        .toSet()
        .map { fnr -> Ident(fnr) }

}
