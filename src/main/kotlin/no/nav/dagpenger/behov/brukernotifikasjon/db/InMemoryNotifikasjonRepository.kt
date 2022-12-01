package no.nav.dagpenger.behov.brukernotifikasjon.db

internal object InMemoryNotifikasjonRepository : NotifikasjonRepository {
    private val notifikasjoner = mutableMapOf<Nøkkel, Beskjed>()

    override fun lagre(nøkkel: Nøkkel, beskjed: Beskjed) {
        notifikasjoner[nøkkel] = beskjed
    }
}
