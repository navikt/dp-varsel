package no.nav.dagpenger.behov.brukernotifikasjon.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.KafkaTopic
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.Beskjed
import no.nav.tms.varsel.builder.BuilderEnvironment
import org.junit.jupiter.api.Test

internal class NotifikasjonerTest {
    private val brukervarselTopic: KafkaTopic<String, String> = mockk(relaxed = true)
    private val notifikasjoner = Notifikasjoner(
        repository = mockk(relaxed = true),
        brukervarselTopic,
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
            brukervarselTopic.publiser(any(), any())
        }
    }
}
