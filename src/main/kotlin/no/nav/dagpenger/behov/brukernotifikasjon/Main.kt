package no.nav.dagpenger.behov.brukernotifikasjon

import no.nav.dagpenger.behov.brukernotifikasjon.api.notifikasjonApi
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresNotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.AivenConfig
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.KafkaTopic
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.BeskjedTopic
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.DoneTopic
import no.nav.dagpenger.behov.brukernotifikasjon.notifikasjoner.OppgaveTopic
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Ettersendinger
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.KubernetesScretsMottakerkilde
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.NotifikasjonBroadcaster
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.Notifikasjoner
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers.BeskjedRiver
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers.DokumentInnsendtRiver
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers.UtkastRiver
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.rivers.VedtakFraArenaRiver
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import java.util.Properties

private val aivenKafka: AivenConfig = AivenConfig.default

fun main() {
    val env = System.getenv()
    runMigration()
    val beskjedTopic by lazy {
        BeskjedTopic(
            createProducer(aivenKafka.producerConfig(avroProducerConfig)),
            config[brukernotifikasjon_beskjed_topic]
        )
    }
    val oppgaveTopic: OppgaveTopic by lazy {
        OppgaveTopic(
            createProducer(aivenKafka.producerConfig(avroProducerConfig)),
            config[brukernotifikasjon_oppgave_topic]
        )
    }
    val doneTopic: DoneTopic by lazy {
        DoneTopic(
            createProducer(aivenKafka.producerConfig(avroProducerConfig)),
            config[brukernotifikasjon_done_topic]
        )
    }
    val utkastTopic by lazy {
        KafkaTopic<String, String>(
            createProducer(aivenKafka.producerConfig(stringProducerConfig)),
            config[tms_utkast_topic]
        )
    }
    val notifikasjonRepository = PostgresNotifikasjonRepository(dataSource)
    val notifikasjoner = Notifikasjoner(
        notifikasjonRepository,
        beskjedTopic,
        oppgaveTopic,
        doneTopic
    )
    val mottakereFraKubernetesSecret = KubernetesScretsMottakerkilde()
    val notifikasjonBroadcaster = NotifikasjonBroadcaster(mottakereFraKubernetesSecret, notifikasjoner)
    val ettersendinger = Ettersendinger(notifikasjoner, notifikasjonRepository)

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule {
            notifikasjonApi(notifikasjoner, notifikasjonBroadcaster)
        }
        .build { _, rapidsConnection ->
            BeskjedRiver(rapidsConnection, notifikasjoner)
            DokumentInnsendtRiver(rapidsConnection, ettersendinger, config[soknadsdialogens_url].toURL())
            VedtakFraArenaRiver(rapidsConnection, ettersendinger)
            UtkastRiver(rapidsConnection, utkastTopic)
        }.start()
}

private fun <K, V> createProducer(producerConfig: Properties = Properties()) =
    KafkaProducer<K, V>(producerConfig).also { producer ->
        Runtime.getRuntime().addShutdownHook(
            Thread {
                producer.flush()
                producer.close()
            }
        )
    }
