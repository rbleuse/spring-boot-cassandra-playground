plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.0-RC1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.rbleuse"
version = "0.0.1-SNAPSHOT"
description = "spring-boot-cassandra6"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven {
        name = "centralSnapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent { snapshotsOnly() }
    }
}

extra["flyway.version"] = "12.7.0"

dependencyManagement {
    imports {
        mavenBom("io.github.rbleuse:spring-boot-flyway-nc-dependencies:1.0.0-SNAPSHOT")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.rbleuse:spring-boot-starter-flyway-nc-cassandra")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-data-cassandra-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers-cassandra")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.1.11")
    testImplementation("io.github.rbleuse:spring-boot-starter-flyway-nc-cassandra-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    environment("FLYWAY_NATIVE_CONNECTORS", "true")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    environment("FLYWAY_NATIVE_CONNECTORS", "true")
}
