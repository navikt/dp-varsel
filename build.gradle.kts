plugins {
    kotlin("jvm") version "1.8.21"
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
}

dependencies {
    val ktorVersion = "2.3.0"

    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    testImplementation(kotlin("test"))

    implementation("com.github.navikt:rapids-and-rivers:2022112407251669271100.df879df951cf")
    implementation("io.github.microutils:kotlin-logging:3.0.4")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.confluent:kafka-avro-serializer:7.3.3")
    implementation("org.apache.avro:avro:1.11.1")

    implementation("com.github.navikt:brukernotifikasjon-schemas:v2.5.2")
    implementation("com.github.navikt:tms-utkast:20230203100430-ecf5208")

    implementation("org.flywaydb:flyway-core:9.16.3")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.5.1")
    implementation("com.github.seratch:kotliquery:1.9.0")

    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.testcontainers:postgresql:1.19.0")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
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
