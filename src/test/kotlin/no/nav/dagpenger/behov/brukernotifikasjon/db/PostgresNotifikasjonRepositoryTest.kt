package no.nav.dagpenger.behov.brukernotifikasjon.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behov.brukernotifikasjon.db.Postgres.withMigratedDb
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostgresNotifikasjonRepositoryTest {
    @Test
    fun `Lagre beskjed`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            lagre(
                Beskjed(
                    eventId = UUID.randomUUID(),
                    ident = Ident("12345678901"),
                    tekst = "tekst",
                    opprettet = LocalDateTime.now(),
                    sikkerhetsnivå = 3,
                    eksternVarsling = false,
                    link = URL("https://www.nav.no")
                )
            )

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("beskjed"))
        }
    }

    @Test
    fun `Lagre oppgave`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            lagre(
                Oppgave(
                    eventId = UUID.randomUUID(),
                    ident = Ident("12345678901"),
                    tekst = "oppgavetekst",
                    opprettet = LocalDateTime.now(),
                    sikkerhetsnivå = 3,
                    eksternVarsling = false,
                    link = URL("https://www.nav.no"),
                    søknadId = UUID.randomUUID()
                )
            )

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("oppgave"))
            assertEquals(0, getAntallRader("beskjed"))
        }
    }

    @Test
    fun `Hente oppgaver for en ident`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val ident1 = Ident("12345678901")
            val expectedOppgave1ForId1 = createOppgaveFor(ident1)
            val expectedOppgave2ForId1 = createOppgaveFor(ident1)
            lagre(expectedOppgave1ForId1)
            lagre(expectedOppgave2ForId1)

            val ident2 = Ident("98765432107")
            lagre(createOppgaveFor(ident2))

            val oppgaverForId1 = hentOppgaver(ident1)
            assertEquals(2, oppgaverForId1.size)
            assertTrue(oppgaverForId1.containsAll(listOf(expectedOppgave1ForId1, expectedOppgave2ForId1)))

            val oppgaverForId2 = hentOppgaver(ident2)
            assertEquals(1, oppgaverForId2.size)
        }
    }

    @Test
    fun `Hente oppgaver knyttet til en konkret søknad og en ident`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val ident = Ident("12345678901")
            val expectedSøknadsId = UUID.randomUUID()

            val expectedOppgave1 = createOppgaveFor(ident, expectedSøknadsId)
            val expectedOppgave2 = createOppgaveFor(ident, expectedSøknadsId)
            lagre(expectedOppgave1)
            lagre(expectedOppgave2)
            lagre(createOppgaveFor(ident))

            val oppgaverTilknyttetSøknaden = hentOppgaver(ident, expectedSøknadsId)
            assertEquals(2, oppgaverTilknyttetSøknaden.size)
            assertTrue(oppgaverTilknyttetSøknaden.containsAll(listOf(expectedOppgave1, expectedOppgave2)))

            assertEquals(3, hentOppgaver(ident).size)
        }
    }

    private fun createOppgaveFor(ident: Ident, søknadId: UUID = UUID.randomUUID()): Oppgave {
        val originalOppgave = Oppgave(
            eventId = UUID.randomUUID(),
            ident = ident,
            tekst = "oppgavetekst",
            opprettet = LocalDateTime.now(),
            sikkerhetsnivå = 3,
            eksternVarsling = false,
            link = URL("https://www.nav.no"),
            søknadId = søknadId
        )
        return originalOppgave
    }

    @Test
    fun `duplikate nøkler feiler for beskjed`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val uuid = UUID.randomUUID()
            lagre(Beskjed(Ident("12345678901"), uuid, "tekst"))

            assertThrows<IllegalArgumentException> {
                lagre(Beskjed(Ident("12345678901"), uuid, "tekst"))
            }

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("beskjed"))
        }
    }

    @Test
    fun `duplikate nøkler feiler for oppgave`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val uuid = UUID.randomUUID()
            val søknadId = UUID.randomUUID()
            lagre(Oppgave(Ident("12345678901"), uuid, URL("https://dummyOppgave"), "oppgavetekst", søknadId))

            assertThrows<IllegalArgumentException> {
                lagre(Oppgave(Ident("12345678901"), uuid, URL("https://dummyOppgave"), "oppgavetekst", søknadId))
            }

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("oppgave"))
        }
    }

    private fun getAntallRader(tabell: String) = sessionOf(dataSource).use { session ->
        session.run(queryOf("select count (*) from $tabell").map { it.int(1) }.asSingle)
    }
}
