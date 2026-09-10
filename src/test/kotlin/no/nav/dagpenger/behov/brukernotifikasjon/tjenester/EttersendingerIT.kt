package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import no.nav.dagpenger.behov.brukernotifikasjon.db.OppgaveObjectMother.giveMeOppgave
import no.nav.dagpenger.behov.brukernotifikasjon.db.Postgres.withMigratedDb
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresNotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.KafkaTopic
import no.nav.tms.varsel.builder.BuilderEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class EttersendingerIT {

    private val brukervarselTopic = mockk<KafkaTopic<String, String>>(relaxed = true)

    @BeforeEach
    fun reset() {
        clearAllMocks()
        BuilderEnvironment.extend(
            mapOf(
                "NAIS_CLUSTER_NAME" to "dev-fss",
                "NAIS_APP_NAME" to "dp-varsel",
                "NAIS_NAMESPACE" to "teamdagpenger",
            )
        )
    }

    @Test
    fun `Skal kunne opprette en ny oppgave for den samme søknaden etter at en tidligere oppgave er løst`() =
        withMigratedDb {
            val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
            val notifikasjoner = Notifikasjoner(repo, brukervarselTopic)
            val ettersendinger = Ettersendinger(notifikasjoner, repo)

            val søknadId = UUID.randomUUID()
            val ident = Ident("11111111111")
            val oppgaveForSammeSøknad1 = giveMeOppgave(ident = ident, søknadId = søknadId)
            ettersendinger.opprettOppgave(oppgaveForSammeSøknad1)
            assertEquals(1, repo.hentAktiveOppgaver(ident, søknadId).size)

            ettersendinger.markerOppgaveSomUtført(EttersendingUtført(ident, søknadId, LocalDateTime.now()))
            assertEquals(0, repo.hentAktiveOppgaver(ident, søknadId).size)

            val oppgaveForSammeSøknad2 = oppgaveForSammeSøknad1.copy(eventId = UUID.randomUUID())
            ettersendinger.opprettOppgave(oppgaveForSammeSøknad2)
            assertEquals(1, repo.hentAktiveOppgaver(ident, søknadId).size)
        }

    @Test
    fun `Skal kunne har flere oppgaver, hvis søker har flere innsendte søknader`() = withMigratedDb {
        val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
        val notifikasjoner = Notifikasjoner(repo, brukervarselTopic)
        val ettersendinger = Ettersendinger(notifikasjoner, repo)

        val ident = Ident("11111111111")
        val søknadId1 = UUID.randomUUID()
        val søknadId2 = UUID.randomUUID()

        ettersendinger.opprettOppgave(giveMeOppgave(ident = ident, søknadId = søknadId1))
        ettersendinger.opprettOppgave(giveMeOppgave(ident = ident, søknadId = søknadId2))

        assertEquals(1, repo.hentAktiveOppgaver(ident, søknadId1).size)
        assertEquals(1, repo.hentAktiveOppgaver(ident, søknadId2).size)
        assertEquals(2, repo.hentAlleAktiveOppgaver(ident).size)
    }

    @Test
    fun `Skal ikke opprette ny oppgave etter ferdigbehandling, selv uten aktiv oppgave`() = withMigratedDb {
        val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
        val notifikasjoner = Notifikasjoner(repo, brukervarselTopic)
        val ettersendinger = Ettersendinger(notifikasjoner, repo)

        val ident = Ident("11111111111")
        val søknadId = UUID.randomUUID()

        ettersendinger.søknadsbehandlingFerdig(ident, søknadId, LocalDateTime.now())
        assertTrue(repo.harFerdigbehandletSøknad(ident, søknadId))

        ettersendinger.opprettOppgave(giveMeOppgave(ident = ident, søknadId = søknadId))

        assertEquals(0, repo.hentAktiveOppgaver(ident, søknadId).size)
    }

    @Test
    fun `Skal fortsatt opprette oppgave for en annen søknad`() = withMigratedDb {
        val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
        val notifikasjoner = Notifikasjoner(repo, brukervarselTopic)
        val ettersendinger = Ettersendinger(notifikasjoner, repo)

        val ident = Ident("11111111111")
        val låstSøknadId = UUID.randomUUID()
        val annenSøknadId = UUID.randomUUID()

        ettersendinger.søknadsbehandlingFerdig(ident, låstSøknadId, LocalDateTime.now())
        ettersendinger.opprettOppgave(giveMeOppgave(ident = ident, søknadId = annenSøknadId))

        assertEquals(1, repo.hentAktiveOppgaver(ident, annenSøknadId).size)
    }

    @Test
    fun `Skal markere søknaden ferdig selv om oppgaven allerede er utløpt`() = withMigratedDb {
        val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
        val notifikasjoner = Notifikasjoner(repo, brukervarselTopic)
        val ettersendinger = Ettersendinger(notifikasjoner, repo)

        val ident = Ident("11111111111")
        val søknadId = UUID.randomUUID()
        val oppgave = giveMeOppgave(ident = ident, søknadId = søknadId)

        ettersendinger.opprettOppgave(oppgave)
        assertEquals(1, repo.hentAktiveOppgaver(ident, søknadId).size)

        ettersendinger.markerSomUtløpt(oppgave.getSnapshot().eventId)
        assertEquals(0, repo.hentAktiveOppgaver(ident, søknadId).size)

        ettersendinger.søknadsbehandlingFerdig(ident, søknadId, LocalDateTime.now())

        assertTrue(repo.harFerdigbehandletSøknad(ident, søknadId))

        ettersendinger.opprettOppgave(giveMeOppgave(ident = ident, søknadId = søknadId))

        assertEquals(0, repo.hentAktiveOppgaver(ident, søknadId).size)
    }

    @Test
    fun `Skal publisere inaktiveringsmelding til brukervarsel-topic ved ferdigbehandling`() = withMigratedDb {
        val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
        val notifikasjoner = Notifikasjoner(repo, brukervarselTopic)
        val ettersendinger = Ettersendinger(notifikasjoner, repo)

        val ident = Ident("11111111111")
        val søknadId = UUID.randomUUID()
        val oppgave = giveMeOppgave(ident = ident, søknadId = søknadId)

        ettersendinger.opprettOppgave(oppgave)
        ettersendinger.søknadsbehandlingFerdig(ident, søknadId, LocalDateTime.now())

        verify(exactly = 2) {
            brukervarselTopic.publiser(any(), any())
        }
    }

    @Test
    fun `Skal ikke blokkere en annen ident med samme søknadId`() = withMigratedDb {
        val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
        val notifikasjoner = Notifikasjoner(repo, brukervarselTopic)
        val ettersendinger = Ettersendinger(notifikasjoner, repo)

        val låstSøknadId = UUID.randomUUID()
        val ident1 = Ident("11111111111")
        val ident2 = Ident("22222222222")

        ettersendinger.søknadsbehandlingFerdig(ident1, låstSøknadId, LocalDateTime.now())
        ettersendinger.opprettOppgave(giveMeOppgave(ident = ident2, søknadId = låstSøknadId))

        assertEquals(1, repo.hentAktiveOppgaver(ident2, låstSøknadId).size)
    }

}
