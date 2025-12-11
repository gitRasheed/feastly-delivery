plugins {
	kotlin("jvm") version "2.0.21"
	kotlin("plugin.spring") version "2.0.21"
	id("org.springframework.boot") version "3.4.7"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.0.21"
	id("io.gitlab.arturbosch.detekt") version "1.23.8"
	id("com.github.spotbugs") version "6.0.26"
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
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testRuntimeOnly("com.h2database:h2")
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

// Detekt: use project config, report issues but don't fail build
detekt {
	config.setFrom(file("detekt.yml"))
	ignoreFailures = true
}

// SpotBugs: report issues but don't fail build (prototype phase)
spotbugs {
	ignoreFailures = true
	excludeFilter.set(file("spotbugs-exclude.xml"))
}
