plugins {
    id("java")
    id("antlr")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.antlr:antlr4-runtime:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    implementation("com.github.javaparser:javaparser-core:3.27.1")
}


tasks.test {
    useJUnitPlatform()
}

tasks.generateGrammarSource {
    arguments.addAll(listOf(
        "-visitor", "-listener",
        "-package", "org.example"
    ))
}