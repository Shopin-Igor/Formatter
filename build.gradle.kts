plugins {
    id("java")
    id("antlr")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    implementation("com.github.javaparser:javaparser-core:3.27.1")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")

}

tasks.test {
    useJUnitPlatform()
}

tasks.generateGrammarSource {
    outputDirectory = file("$buildDir/generated-src/antlr/main/org/example")

    arguments.addAll(
        listOf(
            "-visitor", "-listener",
            "-package", "org.example"
        )
    )
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated-src/antlr/main")
        }
    }
}

