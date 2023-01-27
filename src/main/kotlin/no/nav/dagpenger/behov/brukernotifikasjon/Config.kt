package no.nav.dagpenger.behov.brukernotifikasjon

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import com.natpryce.konfig.uriType
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

val config = EnvironmentVariables() overriding
    systemProperties() overriding
    ConfigurationMap(
        "nais_app_name" to "dp-varsel",
        "nais_namespace" to "teamdagpenger"
    )
val nais_app_name by stringType
val nais_namespace by stringType
val brukernotifikasjon_beskjed_topic by stringType
val brukernotifikasjon_oppgave_topic by stringType
val brukernotifikasjon_done_topic by stringType
val soknadsdialogens_url by uriType
val tms_utkast_topic by stringType

internal val stringProducerConfig by lazy {
    Properties().apply {
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
    }
}
internal val avroProducerConfig by lazy {
    Properties().apply {
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
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
    }
}
