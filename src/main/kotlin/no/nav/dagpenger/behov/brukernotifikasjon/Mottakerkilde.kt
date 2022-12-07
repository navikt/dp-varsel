package no.nav.dagpenger.behov.brukernotifikasjon

import java.io.File

internal interface Mottakerkilde {

    fun hentMottakere(): List<Ident> {
        return lesInnFraKilden()
            .toSet()
            .filter { it.isNotEmpty() }
            .map { fnr -> Ident(fnr) }
    }

    fun lesInnFraKilden(): List<String>
}

class KubernetesScretsMotakerkilde(
    private val pathToSecret: String = "/var/run/secrets/notifikasjon-broadcast-beskjed/beskjedReceivers.txt"
) : Mottakerkilde {
    override fun lesInnFraKilden(): List<String> =
        File(pathToSecret)
            .bufferedReader()
            .readLines()
}
