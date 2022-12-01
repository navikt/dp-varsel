package no.nav.dagpenger.behov.brukernotifikasjon.api

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.behov.brukernotifikasjon.Notifikasjoner
import kotlin.test.Test

class NotifikasjonApiTest {
    private val notifikasjoner = Notifikasjoner(
        beskjedTopic = mockk(relaxed = true)
    )

    @Test
    fun testGetBeskjed() = testApplication {
        application {
            notifikasjonApi(notifikasjoner)
        }
        client.get("/beskjed").apply {
            TODO("Please write your test here")
        }
    }

    @Test
    fun testPostBeskjed() = testApplication {
        application {
            notifikasjonApi(notifikasjoner)
        }
        client.post("/beskjed").apply {
            TODO("Please write your test here")
        }
    }

    @Test
    fun testGetBeskjedId() = testApplication {
        application {
            notifikasjonApi(notifikasjoner)
        }
        client.get("/beskjed/{id").apply {
            TODO("Please write your test here")
        }
    }
}
