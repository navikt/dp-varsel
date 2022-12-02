package no.nav.dagpenger.behov.brukernotifikasjon

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import no.nav.dagpenger.behov.brukernotifikasjon.api.notifikasjonApi
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behov.brukernotifikasjon.db.PostgresNotifikasjonRepository
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.AivenConfig
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic.Companion.beskjedTopic
import no.nav.dagpenger.behov.brukernotifikasjon.kafka.NotifikasjonTopic.Companion.oppgaveTopic
import no.nav.dagpenger.behov.brukernotifikasjon.tjenester.BeskjedRiver
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

private val aivenKafka: AivenConfig = AivenConfig.default
private val avroProducerConfig = Properties().apply {
    val schemaRegistryUser =
        requireNotNull(System.getenv("KAFKA_SCHEMA_REGISTRY_USER")) { "Expected KAFKA_SCHEMA_REGISTRY_USER" }
    val schemaRegistryPassword =
        requireNotNull(System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD")) { "Expected KAFKA_SCHEMA_REGISTRY_PASSWORD" }

    put(
        KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
        requireNotNull(System.getenv("KAFKA_SCHEMA_REGISTRY")) { "Expected KAFKA_SCHEMA_REGISTRY" }
    )
    put(KafkaAvroSerializerConfig.USER_INFO_CONFIG, "$schemaRegistryUser:$schemaRegistryPassword")
    put(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
}

fun main() {
    val env = System.getenv()
    val beskjedTopic by lazy {
        beskjedTopic(
            createProducer(aivenKafka.producerConfig(avroProducerConfig)),
            config[brukernotifikasjon_beskjed_topic]
        )
    }
    val oppgaveTopic by lazy {
        oppgaveTopic(
            createProducer(aivenKafka.producerConfig(avroProducerConfig)),
            config[brukernotifikasjon_oppgave_topic]
        )
    }
    val notifikasjoner = Notifikasjoner(
        PostgresNotifikasjonRepository(dataSource),
        beskjedTopic,
        oppgaveTopic
    )

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule {
            notifikasjonApi(notifikasjoner)
        }
        .build { _, rapidsConnection ->
            BeskjedRiver(rapidsConnection, notifikasjoner)
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
