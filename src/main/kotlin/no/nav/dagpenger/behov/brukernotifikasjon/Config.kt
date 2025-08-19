package no.nav.dagpenger.behov.brukernotifikasjon

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import com.natpryce.konfig.uriType
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
val brukervarsel_topic by stringType
val soknadsdialogens_url by uriType
val tms_utkast_topic by stringType

internal val stringProducerConfig by lazy {
    Properties().apply {
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
    }
}
