package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotifikasjonBroadcasterTest {
    @Test
    fun `Skal ikke produsere noe under et dry run`() {
        val notifikasjoner = mockk<Notifikasjoner>()
        val broadcaster = NotifikasjonBroadcaster(LokalMottakerkilde(), notifikasjoner)
        val antallBeskjederSendt = broadcaster.sendBeskjedTilAlleIdenterISecreten(true)

        assertEquals(0, antallBeskjederSendt.success)
        assertEquals(0, antallBeskjederSendt.feilet)
        assertEquals(3, antallBeskjederSendt.skulleProdusert)
    }

    @Test
    fun `Skal kunne bestille beskjder til alle identer`() {
        val notifikasjoner = mockk<Notifikasjoner>(relaxed = true)
        val broadcaster = NotifikasjonBroadcaster(LokalMottakerkilde(), notifikasjoner)
        val antallBeskjederSendt = broadcaster.sendBeskjedTilAlleIdenterISecreten(false)

        assertEquals(3, antallBeskjederSendt.success)
        assertEquals(0, antallBeskjederSendt.feilet)
        assertEquals(3, antallBeskjederSendt.skulleProdusert)
    }

    @Test
    fun `Skal fortsette å produsere hvis enkelt beskjeder feiler`() {
        val notifikasjoner = mockk<Notifikasjoner>()
        every { notifikasjoner.send(any<Beskjed>()) } throws (Exception("Simulert feil i en test")) andThen Unit
        val broadcaster = NotifikasjonBroadcaster(LokalMottakerkilde(), notifikasjoner)
        val oppsummering = broadcaster.sendBeskjedTilAlleIdenterISecreten(false)

        assertEquals(2, oppsummering.success)
        assertEquals(1, oppsummering.feilet)
        assertEquals(3, oppsummering.skulleProdusert)
    }
}
