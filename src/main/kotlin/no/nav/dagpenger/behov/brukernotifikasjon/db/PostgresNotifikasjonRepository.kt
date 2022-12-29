package no.nav.dagpenger.behov.brukernotifikasjon.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed.BeskjedSnapshot
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Oppgave.OppgaveSnapshot
import java.math.BigInteger
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
        INSERT INTO oppgave (nokkel, tekst, opprettet, sikkerhetsnivaa, ekstern_varsling, link)
        VALUES (:nokkel, :tekst, :opprettet, :sikkerhetsnivaa, :eksternVarsling, :link)
        ON CONFLICT DO NOTHING
        """.trimIndent(),
        mapOf(
            "nokkel" to nøkkelPK,
            "tekst" to oppgave.tekst,
            "opprettet" to oppgave.opprettet,
            "sikkerhetsnivaa" to oppgave.sikkerhetsnivå,
            "eksternVarsling" to oppgave.eksternVarsling,
            "link" to oppgave.link.toString()
        )
    )

}
