import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
}

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.1.3")
    implementation("io.ktor:ktor-server-core-jvm:2.1.3")
    implementation("io.ktor:ktor-serialization-jackson-jvm:2.1.3")
    testImplementation(kotlin("test"))

    implementation("com.github.navikt:rapids-and-rivers:2022112407251669271100.df879df951cf")
    implementation("io.github.microutils:kotlin-logging:3.0.4")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("org.apache.avro:avro:1.11.0")
    implementation("io.confluent:kafka-avro-serializer:7.2.2")

    implementation("com.github.navikt:brukernotifikasjon-schemas:v2.5.2")

    implementation("io.getunleash:unleash-client-java:7.0.0")

    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.1.3")
    // testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.21")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("no.nav.dagpenger.behov.brukernotifikasjon.MainKt")
}
