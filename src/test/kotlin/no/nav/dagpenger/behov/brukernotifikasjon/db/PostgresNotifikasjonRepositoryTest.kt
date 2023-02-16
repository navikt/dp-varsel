package no.nav.dagpenger.behov.brukernotifikasjon.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behov.brukernotifikasjon.db.OppgaveObjectMother.giveMeOppgave
import no.nav.dagpenger.behov.brukernotifikasjon.db.Postgres.withMigratedDb
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PostgresNotifikasjonRepositoryTest {
    @Test
    fun `Lagre beskjed`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            lagre(
                Beskjed(
                    eventId = UUID.randomUUID(),
                    ident = Ident("***********"),
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
            val originalOppgave = giveMeOppgave()
            lagre(originalOppgave)

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("oppgave"))
            assertEquals(0, getAntallRader("beskjed"))

            val original = originalOppgave.getSnapshot()
            val aktiveOppgaver = hentAktiveOppgaver(original.ident, original.søknadId)
            val persistert = aktiveOppgaver[0].getSnapshot()
            assertEquals(original.eventId, persistert.eventId)
            assertEquals(original.ident, persistert.ident)
            assertEquals(original.tekst, persistert.tekst)
            assertNotNull(persistert.opprettet)
            assertEquals(original.sikkerhetsnivå, persistert.sikkerhetsnivå)
            assertEquals(original.eksternVarsling, persistert.eksternVarsling)
            assertEquals(original.link, persistert.link)
            assertEquals(original.søknadId, persistert.søknadId)
            assertEquals(original.aktiv, persistert.aktiv)
            assertNull(persistert.deaktiveringstidspunkt)
            assertNotNull(persistert.synligFramTil)
        }
    }

    @Test
    fun `Skal kunne lagre flere aktive oppgaver for en bruker`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val oppgave1 = giveMeOppgave(søknadId = UUID.randomUUID())
            val oppgave2 = giveMeOppgave(søknadId = UUID.randomUUID())
            lagre(oppgave1)
            lagre(oppgave2)

            assertEquals(2, getAntallRader("nokkel"))
            assertEquals(2, getAntallRader("oppgave"))
            assertEquals(0, getAntallRader("beskjed"))
        }
    }

    @Test
    fun `Hente aktive oppgaver knyttet til en konkret søknad og bruker`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val ident = Ident("***********")
            val expectedSøknadId = UUID.randomUUID()

            val expectedOppgave1 = giveMeOppgave(ident = ident, søknadId = expectedSøknadId)
            val inaktivOppgave = giveMeOppgave(ident = ident, søknadId = expectedSøknadId, aktiv = false)
            val oppgaveKnyttetTilAnnenSøknad = giveMeOppgave(ident)
            lagre(expectedOppgave1)
            lagre(inaktivOppgave)
            lagre(oppgaveKnyttetTilAnnenSøknad)

            val aktiveOppgaverTilknyttetSøknaden = hentAktiveOppgaver(ident, expectedSøknadId)
            assertEquals(1, aktiveOppgaverTilknyttetSøknaden.size)
            assertEquals(
                expectedOppgave1.getSnapshot().eventId,
                aktiveOppgaverTilknyttetSøknaden[0].getSnapshot().eventId
            )
        }
    }

    @Test
    fun `Hente oppgave knyttet til en konkret eventId`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val ident = Ident("***********")
            val expectedEventId = UUID.randomUUID()

            val oppgave1 = giveMeOppgave(ident = ident, eventId = expectedEventId)
            val annenOppgave = giveMeOppgave(ident)
            lagre(oppgave1)
            lagre(annenOppgave)

            val hentetOppgave = hentOppgave(expectedEventId)
            assertNotNull(hentetOppgave)
            assertEquals(expectedEventId, hentetOppgave.getSnapshot().eventId)
        }
    }

    @Test
    fun `Kast feil hvis det ikke finnes en oppgave med den angitte eventId-en`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val ident = Ident("***********")
            val expectedEventId = UUID.randomUUID()

            val aktivOppgave = giveMeOppgave(ident = ident)
            val inaktivOppgave = giveMeOppgave(ident = ident, aktiv = false)
            lagre(aktivOppgave)
            lagre(inaktivOppgave)

            assertThrows<IllegalArgumentException> {
                hentOppgave(expectedEventId)
            }
        }
    }

    @Test
    fun `Hente alle aktive oppgaver for en bruker`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val ident = Ident("***********")

            val expectedEventId1 = UUID.randomUUID()
            val expectedEventId2 = UUID.randomUUID()
            val inaktivOppgave = giveMeOppgave(ident = ident, aktiv = false)
            val oppgaveTilAnnenBruker = giveMeOppgave(ident = Ident("45678901234"))
            lagre(giveMeOppgave(ident = ident, søknadId = expectedEventId1))
            lagre(giveMeOppgave(ident = ident, søknadId = expectedEventId2))
            lagre(inaktivOppgave)
            lagre(oppgaveTilAnnenBruker)

            val aktiveOppgaver = hentAlleAktiveOppgaver(ident)
            assertEquals(2, aktiveOppgaver.size)
            assertNotNull(aktiveOppgaver.find { it.getSnapshot().søknadId == expectedEventId1 })
            assertNotNull(aktiveOppgaver.find { it.getSnapshot().søknadId == expectedEventId2 })
        }
    }

    @Test
    fun `duplikate nøkler feiler for beskjed`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val uuid = UUID.randomUUID()
            lagre(Beskjed(Ident("***********"), uuid, "tekst"))

            assertThrows<IllegalArgumentException> {
                lagre(Beskjed(Ident("***********"), uuid, "tekst"))
            }

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("beskjed"))
        }
    }

    @Test
    fun `duplikate nøkler feiler for oppgave`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val eventId = UUID.randomUUID()
            val søknadId = UUID.randomUUID()
            val oppgaveMedSammeIdEr = giveMeOppgave(eventId = eventId, søknadId = søknadId)
            lagre(oppgaveMedSammeIdEr)

            assertThrows<IllegalArgumentException> {
                lagre(oppgaveMedSammeIdEr)
            }

            assertEquals(1, getAntallRader("nokkel"))
            assertEquals(1, getAntallRader("oppgave"))
        }
    }

    @Test
    fun `Et done-event skal deaktivere et korresponderende aktivt oppgave-event`() = withMigratedDb {
        with(PostgresNotifikasjonRepository(dataSource)) {
            val ident = Ident("98765432101")
            val eventId = UUID.randomUUID()
            val søknadId = UUID.randomUUID()
            lagre(giveMeOppgave(ident = ident, eventId = eventId, søknadId = søknadId))

            val oppgaver = hentAktiveOppgaver(ident, søknadId)
            assertEquals(1, oppgaver.size)

            val grunn = Done.Grunn.FERDIG
            val doneEventForOppgave = Done(ident, eventId, LocalDateTime.now(), grunn, Done.Eventtype.OPPGAVE)
            lagre(doneEventForOppgave)

            val oppgaverEtterDeaktivering = hentInaktiveOppgaver(ident, søknadId)
            assertEquals(1, oppgaverEtterDeaktivering.size)
            val deaktivertOppgave = oppgaverEtterDeaktivering[0].getSnapshot()
            assertNotNull(deaktivertOppgave.deaktiveringstidspunkt)
            assertEquals(grunn, deaktivertOppgave.deaktiveringsgrunn)
        }
    }

    private fun getAntallRader(tabell: String) = sessionOf(dataSource).use { session ->
        session.run(queryOf("select count (*) from $tabell").map { it.int(1) }.asSingle)
    }
}
