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
import no.nav.dagpenger.behov.brukernotifikasjon.brukerdialog_url

internal class UtkastRiverTest {
    private val topic = TestTopic()
    private val rapid by lazy {
        TestRapid().apply {
            UtkastRiver(this, topic)
        }
    }
    private val søknadsdialogUrl = "https://nav.no/soknadsdialog"
    private val brukerdialogUrl = "https://nav.no/brukerdialog"

    init {
        System.setProperty(soknadsdialogens_url.name, søknadsdialogUrl)
        System.setProperty(brukerdialog_url.name, brukerdialogUrl)
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
            assertContains(field(0, "link").asText(), søknadsdialogUrl)

            assertTrue(message(0).has("metrics"))
            with(message(0).get("metrics")) {
                assertTrue(isContainerNode)
                assertTrue(has("skjemanavn"))
                assertTrue(has("skjemakode"))
            }
        }
    }


    @Test
    fun `skal publisere opprettet for brukerdialog`() {
        val søknadId1 = UUID.randomUUID()
        val søknadId2 = UUID.randomUUID()
        rapid.sendTestMessage(tilstandEndretBrukerdialog("Påbegynt", "Dagpenger", søknadId1))
        rapid.sendTestMessage(tilstandEndretBrukerdialog("Påbegynt", "Innsending", søknadId2))

        with(topic.inspektør) {
            assertEquals(1, size)
            assertEquals("created", field(0, "@event_name").asText())
            assertTrue(message(0).has("utkastId"))
            assertTrue(message(0).has("ident"))
            assertTrue(message(0).has("tittel"))
            assertContains(field(0, "link").asText(), "$brukerdialogUrl/$søknadId1/personalia")

            assertTrue(message(0).has("metrics"))
            with(message(0).get("metrics")) {
                assertTrue(isContainerNode)
                assertTrue(has("skjemanavn"))
                assertTrue(has("skjemakode"))
            }
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
    @Test
    fun `skal publisere slettet for brukerdialog`() {
        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(tilstandEndretBrukerdialog("Slettet", "Dagpenger", søknadId))

        with(topic.inspektør) {
            assertEquals(1, size)
            assertEquals("deleted", field(0, "@event_name").asText())
            assertTrue(message(0).has("utkastId"))
            assertContains(field(0, "link").asText(), "$brukerdialogUrl/$søknadId")

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
        assertEquals(skalSendes, SøknadEndretTilstand(message, "").skalPubliseres())
    }


    @ParameterizedTest(name = "{0} av {1} søknad fra Brukerdialog skal sendes: {2}")
    @CsvSource(
        "Påbegynt, Dagpenger, true",
        "Innsendt, Dagpenger, true",
        "Slettet, Dagpenger, true",
        "Påbegynt, Innsending, false",
        "Innsendt, Innsending, true",
        "Slettet, Innsending, true"
    )
    fun `sjekk om pakken fra brukerdialog skal publisers`(tilstand: String, navn: String, skalSendes: Boolean) {
        val søknadId = UUID.randomUUID()
        val message = tilstandEndretBrukerdialog(tilstand, navn, søknadId).let { JsonMessage(it, MessageProblems(it)) }.apply {
            interestedIn("søknad_uuid", "ident", "gjeldendeTilstand", "prosessnavn")
        }
        assertEquals(skalSendes, SøknadEndretTilstand(message, "orkestrator").skalPubliseres())
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

private fun tilstandEndretBrukerdialog(tilstand: String, prosessnavn: String? = null, søknadId: UUID) = JsonMessage.newMessage(
    "søknad_endret_tilstand",
    listOfNotNull(
        "ident" to "12312312312",
        "søknad_uuid" to søknadId,
        "forrigeTilstand" to "Opprettet",
        "kilde" to "orkestrator",
        "gjeldendeTilstand" to tilstand,
        prosessnavn?.let { "prosessnavn" to prosessnavn }
    ).toMap()
).toJson()
