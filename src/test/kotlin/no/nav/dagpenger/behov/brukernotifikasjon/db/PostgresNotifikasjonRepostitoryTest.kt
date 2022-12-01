package no.nav.dagpenger.behov.brukernotifikasjon.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.behov.brukernotifikasjon.db.Postgres.withMigratedDb
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder.dataSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class PostgresNotifikasjonRepostitoryTest {

    @Test
    fun lagre() {
        withMigratedDb {
            PostgresNotifikasjonRepostitory(dataSource).let { repository ->
                val uuid = UUID.randomUUID()
                repository.lagre(Nøkkel(uuid, "12345678901"), Beskjed("tekst"))
                assertThrows<IllegalArgumentException> {
                    repository.lagre(Nøkkel(uuid, "12345678901"), Beskjed("tekst"))
                }

                assertEquals(1, getAntallRader("nokkel"))
                assertEquals(1, getAntallRader("beskjed"))
            }
        }
    }

    private fun getAntallRader(tabell: String) = using(sessionOf(dataSource)) {
        it.run(queryOf("select count (*) from $tabell").map { it.int(1) }.asSingle)
    }
}