plugins {
    kotlin("jvm") version "2.4.20"
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
        // Sårbare logback-versjoner via rapids-and-rivers (GHSA-4c8g-c2f3-jvpq, GHSA-95hg-crgm-wfvg)
        force("ch.qos.logback:logback-core:1.5.34")
        force("ch.qos.logback:logback-classic:1.5.34")
    }
}

dependencies {
    val ktorVersion = "3.4.0"

    implementation(platform("tools.jackson:jackson-bom:3.2.2"))
    implementation("tools.jackson.core:jackson-databind")
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson3-jvm:$ktorVersion")
    testImplementation(kotlin("test"))
    testImplementation("com.github.navikt.rapids-and-rivers:rapids-and-rivers-test:2026071513121784113927")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("com.github.navikt:rapids-and-rivers:2026071513121784113927")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("org.apache.avro:avro:1.12.1")
    implementation("io.confluent:kafka-avro-serializer:7.9.1")

    implementation("com.github.navikt:tms-utkast:20230808103449-2eb1848")
    implementation("no.nav.tms.varsel:kotlin-builder:2.1.1")

    implementation("org.flywaydb:flyway-core:9.22.2")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.12")
    implementation("com.github.seratch:kotliquery:1.9.1")

    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.testcontainers:postgresql:2.0.5")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.1.1")
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
    jvmToolchain(25)
}

application {
    mainClass.set("no.nav.dagpenger.behov.brukernotifikasjon.MainKt")
}
