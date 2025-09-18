plugins {
	kotlin("jvm") version "2.0.21"
	kotlin("plugin.spring") version "2.0.21"
	id("org.springframework.boot") version "3.4.7"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.0.21"
}

group = "com.example.feastly"
version = "0.0.1-SNAPSHOT"
description = "Feastly Delivery App"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.security:spring-security-crypto")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core:10.10.0")
	implementation("org.flywaydb:flyway-database-postgresql:10.10.0")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Testcontainers
	testImplementation("org.testcontainers:junit-jupiter:1.20.2")
	testImplementation("org.testcontainers:postgresql:1.20.2")
	testImplementation("org.testcontainers:testcontainers:1.20.2")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
