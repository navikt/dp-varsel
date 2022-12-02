package no.nav.dagpenger.behov.brukernotifikasjon.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Nøkkel
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.BeskjedMelding
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.OppgaveMelding
import java.math.BigInteger
import javax.sql.DataSource

internal class PostgresNotifikasjonRepository(
    private val dataSource: DataSource
) : NotifikasjonRepository {
    override fun lagre(nøkkel: Nøkkel, beskjed: BeskjedMelding) = sessionOf(dataSource).use { session ->
        val nøkkelPK = session.run(
            lagreNøkkelQuery(nøkkel).map { it.bigDecimal(1).toBigInteger() }.asSingle
        )
        requireNotNull(nøkkelPK) { "Kan ikke lagre dupplikate nøkkler" }
        session.run(
            lagreBeskjedQuery(nøkkelPK, beskjed).asExecute
        )
    }

    override fun lagre(nøkkel: Nøkkel, beskjed: OppgaveMelding): Boolean {
        TODO("Not yet implemented")
    }

    private fun lagreBeskjedQuery(
        nøkkelPK: BigInteger,
        beskjed: BeskjedMelding
    ) = queryOf( //language=PostgreSQL
        """
        INSERT INTO beskjed (nokkel, tekst, opprettet, sikkerhetsnivaa, ekstern_varsling) VALUES (:nokkel, :tekst, :opprettet, :sikkerhetsnivaa, :eksternVarsling) ON CONFLICT DO NOTHING
        """.trimIndent(),
        mapOf(
            "nokkel" to nøkkelPK,
            "tekst" to beskjed.tekst,
            "opprettet" to beskjed.opprettet,
            "sikkerhetsnivaa" to beskjed.sikkerhetsnivå,
            "eksternVarsling" to beskjed.eksternVarsling
        )
    )

    private fun lagreNøkkelQuery(nøkkel: Nøkkel) = queryOf( //language=PostgreSQL
        """
        INSERT INTO nokkel (ident, eventid) VALUES (:ident, :eventId) ON CONFLICT DO NOTHING RETURNING id
        """.trimIndent(),
        mapOf(
            "ident" to nøkkel.ident,
            "eventId" to nøkkel.eventId
        )
    )
}
