package no.nav.dagpenger.behov.brukernotifikasjon.db

import javax.sql.DataSource

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

class PostgresNotifikasjonRepostitory(
    private val dataSource: DataSource
) : NotifikasjonRepository {
    override fun lagre(nøkkel: Nøkkel, beskjed: Beskjed) {
        return using(sessionOf(dataSource)) { session ->
            val nøkkelPK = session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    INSERT INTO nokkel (ident, eventId) VALUES (:ident, :eventId) ON CONFLICT DO NOTHING RETURNING id
                    """.trimIndent(),
                    paramMap = mapOf(
                        "ident" to nøkkel.ident,
                        "eventId" to nøkkel.eventId
                    )
                ).map { it.bigDecimal(1).toBigInteger() }.asSingle
            )
            requireNotNull(nøkkelPK) { "Kan ikke lagre dupplikate nøkkler" }
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    INSERT INTO beskjed (nokkel, tekst, opprettet, sikkerhetsnivaa, ekstern_varsling) VALUES (:nokkel, :tekst, :opprettet, :sikkerhetsnivaa, :eksternVarsling) ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    paramMap = mapOf(
                        "nokkel" to nøkkelPK,
                        "tekst" to beskjed.tekst,
                        "opprettet" to beskjed.opprettet,
                        "sikkerhetsnivaa" to beskjed.sikkerhetsnivå,
                        "eksternVarsling" to beskjed.eksternVarsling
                    )
                ).asExecute
            )
        }

    }

}
