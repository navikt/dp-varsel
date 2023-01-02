package no.nav.dagpenger.behov.brukernotifikasjon

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties

val config = EnvironmentVariables() overriding
    systemProperties() overriding
    ConfigurationMap(
        "nais_app_name" to "dp-behov-brukernotifikasjon",
        "nais_namespace" to "teamdagpenger"
    )

val nais_app_name by stringType
val nais_namespace by stringType
val brukernotifikasjon_beskjed_topic by stringType
val brukernotifikasjon_oppgave_topic by stringType
val brukernotifikasjon_done_topic by stringType
