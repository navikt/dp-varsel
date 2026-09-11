package no.nav.dagpenger.behov.brukernotifikasjon.helpers

import no.nav.dagpenger.behov.brukernotifikasjon.kafka.Topic
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid

class TestTopic : Topic<String, String> {
    private val messages = mutableListOf<Pair<String?, String>>()
    val inspektør get() = TestRapid.RapidInspector(messages.toList())

    override fun publiser(melding: String) {
        messages.add(null to melding)
    }

    override fun publiser(nøkkel: String, melding: String) {
        messages.add(nøkkel to melding)
    }
}
