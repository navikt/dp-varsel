package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.BeskjedTopic
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.DoneTopic
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.OppgaveTopic
import no.nav.tms.varsel.builder.BuilderEnvironment
import org.junit.jupiter.api.Test

internal class NotifikasjonerTest {
    private val beskjedTopic: BeskjedTopic = mockk(relaxed = true)
    private val oppgaveTopic: OppgaveTopic = mockk(relaxed = true)
    private val doneTopic: DoneTopic = mockk(relaxed = true)
    private val notifikasjoner = Notifikasjoner(
        repository = mockk(relaxed = true),
        beskjedTopic,
        oppgaveTopic,
        doneTopic
    )

    @Test
    fun test() {
        BuilderEnvironment.extend(mapOf(
            "NAIS_CLUSTER_NAME" to "dev-fss",
            "NAIS_APP_NAME" to "dp-varsel",
            "NAIS_NAMESPACE" to "teamdagpenger",
        ))

        notifikasjoner.send(
            Beskjed(
                Ident("12312312311"),
                "1-2-3 nå kommer en beskjed"
            )
        )

        verify {
            beskjedTopic.publiser(any(), any())
        }
    }
}
