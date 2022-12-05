package no.nav.dagpenger.behov.brukernotifikasjon

import java.io.File

class NotifikasjonBroadcaster(private val pathToSecret: String = "/var/run/secrets/brukernotifikasjon-broadcast-beskjed/beskjedBroadcastReceivers.txt") {

    fun sendBeskjedTilAlleIdenterISecreten(dryRun: Boolean) {
        println("dry run: $dryRun")
        val identer: List<Ident> = lesInnIdenterFraSecret()
        println("Fant ${identer.size} identer")
        identer.forEachIndexed { index, ident ->
            println("Ident $index: $ident")
        }
        if(!dryRun) {
            println("Skal bestille beskjed til ${identer.size} brukere...")
        }
    }

    private fun lesInnIdenterFraSecret() = File(pathToSecret)
        .bufferedReader()
        .readLines()
        .map { fnr -> Ident(fnr) }

}
