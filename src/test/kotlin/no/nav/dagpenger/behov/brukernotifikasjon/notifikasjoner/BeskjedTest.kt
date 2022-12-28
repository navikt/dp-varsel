package no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner

import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal
import no.nav.dagpenger.behov.brukernotifikasjon.Ident
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BeskjedTest {

    @Test
    fun `Ved ekstern varsling prefererer vi alltid at SMS er kanalen som brukes`() {
        val utenEksternVarsling = Beskjed(Ident("123"), "uten ekstern varsling", false)
        assertEquals(0, utenEksternVarsling.somInput().prefererteKanaler.size)

        val medEksternVarsling = Beskjed(Ident("123"), "med ekstern varsling", true)
        val medEksternVarslingSomInput = medEksternVarsling.somInput()
        assertEquals(1, medEksternVarslingSomInput.prefererteKanaler.size)
        assertEquals(PreferertKanal.SMS.toString(), medEksternVarslingSomInput.prefererteKanaler.first())
    }
}
