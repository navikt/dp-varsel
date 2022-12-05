package no.nav.dagpenger.behov.brukernotifikasjon.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.Ident
import no.nav.dagpenger.behov.brukernotifikasjon.NotifikasjonBroadcaster
import no.nav.dagpenger.behov.brukernotifikasjon.Notifikasjoner
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import kotlin.test.Test

class NotifikasjonApiTest {
    private val notifikasjoner: Notifikasjoner = mockk(relaxed = true)
    private val notifikasjonBroadcaster: NotifikasjonBroadcaster = mockk()
    private val ident = "12312312311"
    private val tekst = "asdfasdf"

    @Test
    @Disabled
    fun testGetBeskjed() = testApplication {
        application {
            notifikasjonApi(notifikasjoner, notifikasjonBroadcaster)
        }
        client.get("/beskjed").apply {
            TODO("Please write your test here")
        }
    }

    @Test
    fun testPostBeskjed() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                }
            }
        }
        application {
            notifikasjonApi(notifikasjoner, notifikasjonBroadcaster)
        }
        client.post("/beskjed") {
            header("Content-Type", Json)
            setBody(
                PostBeskjed(
                    ident,
                    tekst,
                    true
                )
            )
        }.apply {
            val kommando = slot<Beskjed>()

            verify {
                notifikasjoner.send(capture(kommando))
            }

            assertEquals(Ident(ident), kommando.captured.getSnapshot().ident)
            assertEquals(tekst, kommando.captured.getSnapshot().tekst)
        }
    }

    @Test
    @Disabled
    fun testGetBeskjedId() = testApplication {
        application {
            notifikasjonApi(notifikasjoner, notifikasjonBroadcaster)
        }
        client.get("/beskjed/{id").apply {
            TODO("Please write your test here")
        }
    }
}
