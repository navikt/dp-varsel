plugins {
    kotlin("jvm") version "2.1.0"
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
}

configurations.all {
    resolutionStrategy {
        // Sårbar versjon av snappy-java i kafka-avro-serializer:7.5.0 via kafka-clients:3.5.0
        force("org.xerial.snappy:snappy-java:1.1.10.7")
    }
}

dependencies {
    val ktorVersion = "2.3.13"

    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    testImplementation(kotlin("test"))

    implementation("com.github.navikt:rapids-and-rivers:2024020419561707073004.70bfb92c077c")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("org.apache.avro:avro:1.12.0")
    implementation("io.confluent:kafka-avro-serializer:7.8.0")

    implementation("com.github.navikt:brukernotifikasjon-schemas:v2.5.2")
    implementation("com.github.navikt:tms-utkast:20230808103449-2eb1848")

    implementation("org.flywaydb:flyway-core:11.1.1")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.github.seratch:kotliquery:1.9.1")

    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
}

tasks {
    test {
        useJUnitPlatform()
    }
    jar {
        manifest {
            attributes["Main-Class"] = application.mainClass
        }

        archiveBaseName.set("app")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("no.nav.dagpenger.behov.brukernotifikasjon.MainKt")
}
