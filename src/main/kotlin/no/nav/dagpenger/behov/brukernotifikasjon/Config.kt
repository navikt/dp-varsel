package no.nav.dagpenger.behov.brukernotifikasjon

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

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
