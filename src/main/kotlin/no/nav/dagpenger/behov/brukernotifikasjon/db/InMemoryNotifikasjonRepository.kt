package no.nav.dagpenger.behov.brukernotifikasjon.db

import java.util.UUID

internal object InMemoryNotifikasjonRepository : NotifikasjonRepository {
    private val notifikasjoner = mutableMapOf<UUID, Beskjed>()

    override fun lagre(nøkkel: Nøkkel, beskjed: Beskjed) {
        notifikasjoner[UUID.randomUUID()] = beskjed
    }
}

// Nøkkel (felles FK)

// Beskjed
// Oppgave
// Done
