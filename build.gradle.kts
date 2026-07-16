buildscript {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        // The io.antigen Gradle plugin lives in the antigen-cli module (Phase 2b module split).
        // Resolved from JitPack, which builds the tagged release of integralquality/antigen.
        // Multi-module coordinate: com.github.<owner>.<repo>:<module>:<tag>.
        // For local development against unpublished changes, add mavenLocal() above and swap to
        // "io.antigen:antigen-cli:1.0.0-SNAPSHOT" (published via ./gradlew publishToMavenLocal).
        classpath("com.github.integralquality.antigen:antigen-cli:v0.10")
    }
}

plugins {
    id("java")
}
apply(plugin = "io.antigen")

group = "io.example.antigen"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.rest-assured:rest-assured:5.5.6")
    testImplementation("io.rest-assured:json-path:5.3.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    // The JVM adapter; exposes the pure engine transitively via `api`.
    testImplementation("com.github.integralquality.antigen:antigen-test-runner:v0.10")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

tasks.test {
    useJUnitPlatform()

    doFirst {
        System.getProperty("runWithAntigen")?.let { jvmArgs("-DrunWithAntigen=$it") }
        System.getProperty("antigen.report.path")?.let { jvmArgs("-Dantigen.report.path=$it") }
        System.getProperty("antigen.report.json_only")?.let { jvmArgs("-Dantigen.report.json_only=$it") }
        System.getProperty("io.antigen.core.config.source")?.let { jvmArgs("-Dio.antigen.core.config.source=$it") }

        if (System.getProperty("runWithAntigen") == "true") {
            configurations.testRuntimeClasspath.get()
                .files.find { it.name.contains("aspectjweaver") }
                ?.let { jvmArgs("-javaagent:${it.absolutePath}") }
        }
    }
}
