package no.nav.dagpenger.behov.brukernotifikasjon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotifikasjonBroadcasterTest {

    @Test
    fun `Skal kunne bestille beskjder til alle identer, og ignorere duplikater og tomme linjer`() {
        val broadcaster = NotifikasjonBroadcaster(LokalMotakerkilde())
        val antallBeskjederSendt = broadcaster.sendBeskjedTilAlleIdenterISecreten(true)

        assertEquals(3, antallBeskjederSendt)
    }
}
