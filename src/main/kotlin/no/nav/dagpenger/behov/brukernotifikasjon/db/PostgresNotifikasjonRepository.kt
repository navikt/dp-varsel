package no.nav.dagpenger.behov.brukernotifikasjon.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed.BeskjedSnapshot
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Done
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave.OppgaveSnapshot
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import java.math.BigInteger
import java.net.URL
import java.util.*
import javax.sql.DataSource

internal class PostgresNotifikasjonRepository(
    private val dataSource: DataSource
) : NotifikasjonRepository {
    override fun lagre(beskjed: Beskjed): Boolean {
        val nøkkel = beskjed.getNøkkel()
        val data = beskjed.getSnapshot()

        return sessionOf(dataSource).use { session ->
            val nøkkelPK = session.run(
                lagreNøkkelQuery(nøkkel).map { it.bigDecimal(1).toBigInteger() }.asSingle
            )
            requireNotNull(nøkkelPK) { "Kan ikke lagre duplikate nøkler" }
            session.run(
                lagreBeskjedQuery(nøkkelPK, data).asExecute
            )
        }
    }

    override fun lagre(oppgave: Oppgave): Boolean {
        val nøkkel = oppgave.getNøkkel()
        val data = oppgave.getSnapshot()

        return sessionOf(dataSource).use { session ->
            val nøkkelPK = session.run(
                lagreNøkkelQuery(nøkkel).map { it.bigDecimal(1).toBigInteger() }.asSingle
            )
            requireNotNull(nøkkelPK) { "Kan ikke lagre duplikate nøkler" }
            session.run(
                lagreOppgaveQuery(nøkkelPK, data).asExecute
            )
        }
    }

    override fun lagre(done: Done): Boolean {
        val nøkkel = done.getNøkkel()
        val data = done.getSnapshot()

        return sessionOf(dataSource).use { session ->
            val nøkkelPK = session.run(
                finnNøkkelQuery(nøkkel).map { it.bigDecimal(1).toBigInteger() }.asSingle
            )
            requireNotNull(nøkkelPK) { "Eventet som skal markeres som done ble ikke funnet" }
            session.run(
                lagreDoneQuery(nøkkelPK, data).asExecute
            )
        }
    }

    private fun lagreDoneQuery(
        nøkkelPK: BigInteger,
        done: Done.DoneSnapshot
    ): Query {
        val tabell = done.eventtype.name.lowercase()
        return queryOf( //language=PostgreSQL
            """
        UPDATE $tabell SET 
            aktiv = false, 
            deaktiveringstidspunkt = :deaktiveringstidspunkt,
            deaktiveringsgrunn = :deaktiveringsgrunn 
            WHERE nokkel = :nokkel
        """.trimIndent(),
            mapOf(
                "nokkel" to nøkkelPK,
                "deaktiveringstidspunkt" to done.tidspunkt,
                "deaktiveringsgrunn" to done.grunn.toString()
            )
        )
    }

    private fun finnNøkkelQuery(nøkkel: Nøkkel) = queryOf( //language=PostgreSQL
        "SELECT id FROM nokkel WHERE ident = :ident AND eventid = :eventId",
        mapOf(
            "ident" to nøkkel.ident.ident,
            "eventId" to nøkkel.eventId
        )
    )

    override fun hentAktiveOppgaver(ident: Ident, søknadId: UUID): List<Oppgave> = sessionOf(dataSource).use { session ->
        session.run(
            oppgaverForKonkretSøknadQuery(ident, søknadId, true).map {
                it.toOppgave()
            }.asList
        )
    }

    override fun hentAlleAktiveOppgaver(ident: Ident): List<Oppgave> = sessionOf(dataSource).use { session ->
        session.run(
            alleOppgaverForKonkretBrukerQuery(ident).map {
                it.toOppgave()
            }.asList
        )
    }

    internal fun hentInaktiveOppgaver(ident: Ident, søknadId: UUID): List<Oppgave> = sessionOf(dataSource).use { session ->
        session.run(
            oppgaverForKonkretSøknadQuery(ident, søknadId, false).map {
                it.toOppgave()
            }.asList
        )
    }

    private fun oppgaverForKonkretSøknadQuery(ident: Ident, søknadId: UUID, aktiv : Boolean = true) = queryOf( //language=PostgreSQL
        """SELECT * 
            FROM oppgave o 
            JOIN nokkel n ON n.id = o.nokkel
            WHERE n.ident = :ident AND o.soknadId = :soknadId AND aktiv = :aktiv
            """.trimIndent(),
        mapOf(
            "ident" to ident.ident,
            "soknadId" to søknadId,
            "aktiv" to aktiv
        )
    )

    private fun alleOppgaverForKonkretBrukerQuery(ident: Ident, aktiv : Boolean = true) = queryOf( //language=PostgreSQL
        """SELECT * 
            FROM oppgave o 
            JOIN nokkel n ON n.id = o.nokkel
            WHERE n.ident = :ident AND aktiv = :aktiv
            """.trimIndent(),
        mapOf(
            "ident" to ident.ident,
            "aktiv" to aktiv
        )
    )

    private fun Row.toOppgave() = Oppgave(
        ident = Ident(string("ident")),
        søknadId = uuid("soknadId"),
        eventId = uuid("eventId"),
        tekst = string("tekst"),
        opprettet = localDateTime("opprettet"),
        sikkerhetsnivå = int("sikkerhetsnivaa"),
        eksternVarsling = boolean("ekstern_varsling"),
        link = URL(string("link")),
        aktiv = boolean("aktiv"),
        deaktiveringstidspunkt = localDateTimeOrNull("deaktiveringstidspunkt"),
        deaktiveringsgrunn = stringOrNull("deaktiveringsgrunn")?.let { Done.Grunn.valueOf(it) },
        synligFramTil = localDateTime("synligFramTil")
    )

    private fun lagreNøkkelQuery(nøkkel: Nøkkel) = queryOf( //language=PostgreSQL
        """
        INSERT INTO nokkel (ident, eventid) VALUES (:ident, :eventId) ON CONFLICT DO NOTHING RETURNING id
        """.trimIndent(),
        mapOf(
            "ident" to nøkkel.ident.ident,
            "eventId" to nøkkel.eventId
        )
    )

    private fun lagreBeskjedQuery(
        nøkkelPK: BigInteger,
        beskjed: BeskjedSnapshot
    ) = queryOf( //language=PostgreSQL
        """
        INSERT INTO beskjed (nokkel, tekst, opprettet, sikkerhetsnivaa, ekstern_varsling, link)
        VALUES (:nokkel, :tekst, :opprettet, :sikkerhetsnivaa, :eksternVarsling, :link)
        ON CONFLICT DO NOTHING
        """.trimIndent(),
        mapOf(
            "nokkel" to nøkkelPK,
            "tekst" to beskjed.tekst,
            "opprettet" to beskjed.opprettet,
            "sikkerhetsnivaa" to beskjed.sikkerhetsnivå,
            "eksternVarsling" to beskjed.eksternVarsling,
            "link" to beskjed.link.toString()
        )
    )

    private fun lagreOppgaveQuery(
        nøkkelPK: BigInteger,
        oppgave: OppgaveSnapshot
    ) = queryOf( //language=PostgreSQL
        """
        INSERT INTO oppgave (nokkel, tekst, opprettet, sikkerhetsnivaa, ekstern_varsling, link, soknadId, aktiv, synligFramTil)
        VALUES (:nokkel, :tekst, :opprettet, :sikkerhetsnivaa, :eksternVarsling, :link, :soknadId, :aktiv, :synligFramTil)
        ON CONFLICT DO NOTHING
        """.trimIndent(),
        mapOf(
            "nokkel" to nøkkelPK,
            "tekst" to oppgave.tekst,
            "opprettet" to oppgave.opprettet,
            "sikkerhetsnivaa" to oppgave.sikkerhetsnivå,
            "eksternVarsling" to oppgave.eksternVarsling,
            "link" to oppgave.link.toString(),
            "soknadId" to oppgave.søknadId,
            "aktiv" to oppgave.aktiv,
            "synligFramTil" to oppgave.synligFramTil
        )
    )

}
