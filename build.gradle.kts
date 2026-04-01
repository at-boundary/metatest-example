plugins {
    id("java")
}

group = "io.example.java.rest.metatest"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.rest-assured:rest-assured:5.3.0")
    testImplementation("io.rest-assured:json-path:5.3.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    implementation("com.github.at-boundary:metatest:v0.1.4")
    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

tasks.test {
    useJUnitPlatform()

    val aspectjAgent = configurations.testRuntimeClasspath.get()
        .files.find { it.name.contains("aspectjweaver") }

    if (aspectjAgent != null) {
        jvmArgs(
            "-javaagent:$aspectjAgent",
            "-DrunWithMetatest=${System.getProperty("runWithMetatest") ?: "false"}"
        )
    }
}