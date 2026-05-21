plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"

}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "mysawit-be"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Core Web & JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Spring Security + OAuth2 Client
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // JWT (JJWT 0.12.x)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Redis + Cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Retry (optimistic locking retries on wallet operations)
    implementation("org.springframework.retry:spring-retry:2.0.12")

    // Structured JSON logging
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Rate limiting (Redis-backed via bucket4j-redis + Lettuce)
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("com.bucket4j:bucket4j-redis:8.10.1")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // WebFlux (reactive HTTP client for OAuth2 token exchange)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Actuator + Metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Dev
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // AWS SDK (R2 storage)
    implementation("software.amazon.awssdk:s3:2.25.60")

    // DB
    runtimeOnly("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("com.h2database:h2")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    filter {
        excludeTestsMatching("*FunctionalTest")
    }
    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

tasks.register<JavaExec>("changeAdminPassword") {
    group       = "admin"
    description = "Change the admin account password. Pass new password via -Ppassword=<value>."
    classpath   = sourceSets["main"].runtimeClasspath
    mainClass   = "id.ac.ui.cs.advprog.mysawitbe.tools.ChangeAdminPassword"

    val newPassword = project.findProperty("password")?.toString() ?: ""
    args = listOf(newPassword)

    // Forward the same DB env vars the app uses
    environment("DB_URL",      System.getenv("DB_URL")      ?: "jdbc:postgresql://localhost:5432/mysawit")
    environment("DB_USERNAME", System.getenv("DB_USERNAME") ?: "postgres")
    environment("DB_PASSWORD", System.getenv("DB_PASSWORD") ?: "postgres")
}

tasks.register<JavaExec>("seedPayrollTestData") {
    group       = "seed"
    description = "Generate deterministic seed data for payroll and wallet feature testing."
    classpath   = sourceSets["main"].runtimeClasspath
    mainClass   = "id.ac.ui.cs.advprog.mysawitbe.tools.SeedPayrollTestData"

    environment("DB_URL",      System.getenv("DB_URL")      ?: "jdbc:postgresql://localhost:5432/mysawit")
    environment("DB_USERNAME", System.getenv("DB_USERNAME") ?: "postgres")
    environment("DB_PASSWORD", System.getenv("DB_PASSWORD") ?: "postgres")
}
