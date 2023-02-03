package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.db.OppgaveObjectMother.giveMeOppgave
import no.nav.dagpenger.behov.brukernotifikasjon.db.Postgres.withMigratedDb
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresNotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.*
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.BeskjedTopic
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.OppgaveTopic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class EttersendingerIT {

    private val doneTopic = mockk<DoneTopic>(relaxed = true)
    private val oppgaveTopic = mockk<OppgaveTopic>(relaxed = true)
    private val beskjedTopic = mockk<BeskjedTopic>()

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Skal kunne opprette en ny oppgave for den samme søknaden, etter at den tidligere har hatt en oppgave som har blitt løst`() =
        withMigratedDb {
            val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
            val notifikasjoner = Notifikasjoner(repo, beskjedTopic, oppgaveTopic, doneTopic)
            val ettersendinger = Ettersendinger(notifikasjoner, repo)

            val søknadId = UUID.randomUUID()
            val ident = Ident("11111111111")

            val oppgaveForSammeSøknad1 = giveMeOppgave(ident = ident, søknadId = søknadId)
            ettersendinger.opprettOppgave(oppgaveForSammeSøknad1)
            assertEquals(1, repo.hentAktiveOppgaver(ident, søknadId).size)
            verify { oppgaveTopic.publiser(any(), any()) }

            val utførtEttersending = EttersendingUtført(ident, søknadId, LocalDateTime.now())
            ettersendinger.markerOppgaveSomUtført(utførtEttersending)
            assertEquals(0, repo.hentAktiveOppgaver(ident, søknadId).size)
            verify { doneTopic.publiser(any(), any()) }

            val oppgaveForSammeSøknad2 = oppgaveForSammeSøknad1.copy(eventId = UUID.randomUUID())
            ettersendinger.opprettOppgave(oppgaveForSammeSøknad2)
            assertEquals(1, repo.hentAktiveOppgaver(ident, søknadId).size)
            verify { oppgaveTopic.publiser(any(), any()) }
        }

    @Test
    fun `Skal kunne har flere oppgaver, hvis søker har flere innsendte søknader`() = withMigratedDb {
        val repo = PostgresNotifikasjonRepository(PostgresDataSourceBuilder.dataSource)
        val notifikasjoner = Notifikasjoner(repo, beskjedTopic, oppgaveTopic, doneTopic)
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

}
