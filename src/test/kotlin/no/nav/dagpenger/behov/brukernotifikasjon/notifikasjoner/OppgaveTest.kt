package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ident
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.builder.BuilderEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID

class OppgaveTest {

    @BeforeEach
    fun setup() {
        BuilderEnvironment.extend(mapOf(
            "NAIS_CLUSTER_NAME" to "dev-fss",
            "NAIS_APP_NAME" to "dp-varsel",
            "NAIS_NAMESPACE" to "teamdagpenger",
        ))
    }

    @Test
    fun `Ekstern varsling er ikke med i json-meldingen når eksterVarsling er false i Beskjed`() {
        val utenEksternVarsling = Oppgave(
            ident = Ident("12345678901"),
            eventId = UUID.randomUUID(),
            tekst = "Dette er tekst",
            opprettet = LocalDateTime.now(),
            link = URL("https://www.nav.no"),
            søknadId = UUID.randomUUID(),
            synligFramTil = LocalDateTime.now().plusWeeks(3),
            eksternVarsling = false,
            eksternVarslingTekst = null,
        )

        val somInputJson = Json.parseToJsonElement(utenEksternVarsling.somInput())
        assertNull(somInputJson.jsonObject["eksternVarsling"])
    }

    @Test
    fun `Vi bruker SMS som preferert kanal når eksternVarsling er true`() {
        val oppgaveMedEksernVarsling = Oppgave(
            ident = Ident("12345678901"),
            eventId = UUID.randomUUID(),
            tekst = "Dette er tekst",
            opprettet = LocalDateTime.now(),
            link = URL("https://www.nav.no"),
            søknadId = UUID.randomUUID(),
            synligFramTil = LocalDateTime.now().plusWeeks(3),
            eksternVarsling = true,
            eksternVarslingTekst = null,
        )

        val somInputJson = Json.parseToJsonElement(oppgaveMedEksernVarsling.somInput())

        val eksternVarsling = somInputJson.jsonObject["eksternVarsling"]
        val prefererteKanaler = eksternVarsling!!.jsonObject["prefererteKanaler"]!!.jsonArray
        assertEquals(1, prefererteKanaler.size)
        assertEquals("\"${EksternKanal.SMS}\"", prefererteKanaler.first().toString())
        assertNull(eksternVarsling.jsonObject["smsVarslingstekst"])

    }
}