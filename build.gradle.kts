plugins {
    kotlin("jvm") version "2.2.0"
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

configurations.all {
    resolutionStrategy {
        // Sårbar versjon av snappy-java i kafka-avro-serializer:7.5.0 via kafka-clients:3.5.0
        force("org.xerial.snappy:snappy-java:1.1.10.8")
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
    implementation("io.confluent:kafka-avro-serializer:7.9.1")

    implementation("com.github.navikt:tms-utkast:20230808103449-2eb1848")
    implementation("no.nav.tms.varsel:kotlin-builder:2.1.1")

    implementation("org.flywaydb:flyway-core:9.22.2")
    implementation("com.zaxxer:HikariCP:6.3.2")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.github.seratch:kotliquery:1.9.1")

    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.4")
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
