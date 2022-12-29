package no.nav.dagpenger.behov.brukernotifikasjon.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behov.brukernotifikasjon.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.db.Postgres.withMigratedDb
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

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
                    link = URL("https://www.nav.no")
                )
            )

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("oppgave"))
            assertEquals(0, getAntallRader("beskjed"))
        }
    }

    @Test
    fun `duplikate nøkler feiler for beskjed`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val uuid = UUID.randomUUID()
            lagre(Beskjed(uuid, Ident("12345678901"), "tekst"))

            assertThrows<IllegalArgumentException> {
                lagre(Beskjed(uuid, Ident("12345678901"), "tekst"))
            }

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("beskjed"))
        }
    }

    @Test
    fun `duplikate nøkler feiler for oppgave`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val uuid = UUID.randomUUID()
            lagre(Oppgave(uuid, Ident("12345678901"), URL("https://dummyOppgave"),"oppgavetekst"))

            assertThrows<IllegalArgumentException> {
                lagre(Oppgave(uuid, Ident("12345678901"), URL("https://dummyOppgave"),"oppgavetekst"))
            }

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("oppgave"))
        }
    }

    private fun getAntallRader(tabell: String) = sessionOf(dataSource).use { session ->
        session.run(queryOf("select count (*) from $tabell").map { it.int(1) }.asSingle)
    }
}
