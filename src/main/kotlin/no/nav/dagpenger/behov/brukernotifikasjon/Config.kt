package no.nav.dagpenger.behov.brukernotifikasjon

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties

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
val nais_cluster_name by stringType

fun runningInDev() = config[nais_cluster_name].equals("dev-gcp", ignoreCase = true)
