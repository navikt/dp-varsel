package no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers

import no.nav.dagpenger.behov.brukernotifikasjon.helpers.TestTopic
import no.nav.dagpenger.behov.brukernotifikasjon.soknadsdialogens_url
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.UUID
import kotlin.test.assertContains

internal class UtkastRiverTest {
    private val topic = TestTopic()
    private val rapid by lazy {
        TestRapid().apply {
            UtkastRiver(this, topic)
        }
    }
    private val testUrl = "https://nav.no/soknad"

    init {
        System.setProperty(soknadsdialogens_url.name, testUrl)
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal publisere opprettet`() {
        rapid.sendTestMessage(tilstandEndret("Påbegynt", "Dagpenger"))
        rapid.sendTestMessage(tilstandEndret("Påbegynt", "Innsending"))

        with(topic.inspektør) {
            assertEquals(1, size)
            assertEquals("created", field(0, "@event_name").asText())
            assertTrue(message(0).has("utkastId"))
            assertTrue(message(0).has("ident"))
            assertTrue(message(0).has("tittel"))
            assertContains(field(0, "link").asText(), testUrl)
        }
    }

    @Test
    fun `skal publisere slettet`() {
        rapid.sendTestMessage(tilstandEndret("Slettet"))

        with(topic.inspektør) {
            assertEquals(1, size)
            assertEquals("deleted", field(0, "@event_name").asText())
            assertTrue(message(0).has("utkastId"))
        }
    }

    @ParameterizedTest(name = "{0} av {1} skal sendes: {2}")
    @CsvSource(
        "Påbegynt, Dagpenger, true",
        "Innsendt, Dagpenger, true",
        "Slettet, Dagpenger, true",
        "Påbegynt, Innsending, false",
        "Innsendt, Innsending, true",
        "Slettet, Innsending, true"
    )
    fun `sjekk om pakken skal publisers`(tilstand: String, navn: String, skalSendes: Boolean) {
        val message = tilstandEndret(tilstand, navn).let { JsonMessage(it, MessageProblems(it)) }.apply {
            interestedIn("søknad_uuid", "ident", "gjeldendeTilstand", "prosessnavn")
        }
        assertEquals(skalSendes, SøknadEndretTilstand(message).skalPubliseres())
    }
}

private fun tilstandEndret(tilstand: String, prosessnavn: String? = null) = JsonMessage.newMessage(
    "søknad_endret_tilstand",
    listOfNotNull(
        "ident" to "12312312312",
        "søknad_uuid" to UUID.randomUUID(),
        "forrigeTilstand" to "Opprettet",
        "gjeldendeTilstand" to tilstand,
        prosessnavn?.let { "prosessnavn" to prosessnavn }
    ).toMap()
).toJson()
