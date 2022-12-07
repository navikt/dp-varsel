package no.nav.dagpenger.behov.brukernotifikasjon

class LokalMotakerkilde : Mottakerkilde {

    override fun lesInnFraKilden(): List<String> = javaClass.getResource("/beskjedReceivers.txt")
        ?.readText()
        ?.lines()
        ?: throw Exception("Klarte ikke å lese inn fila med testidenter")
}
