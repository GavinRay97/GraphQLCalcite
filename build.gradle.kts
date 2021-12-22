import net.bnb1.kradle.blueprints.JavaBlueprint

plugins {
    id("java")

    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("org.jetbrains.qodana") version "0.1.12"
    id("org.jetbrains.kotlinx.kover") version "0.4.4"

    // https://github.com/mrkuz/kradle
    id("net.bitsandbobs.kradle-app") version "1.2.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(
        "https://repository.apache.org/content/repositories/snapshots"
    )
    maven(
        "https://oss.sonatype.org/content/repositories/snapshots"
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

qodana {
    showReportPort.set(8888)
}

kradle {
    // Disable Java blueprint to be able to use "toolchains" instead
    disable(JavaBlueprint::class.java)
    mainClass("com.example.ExampleKotlin")
    kotlinxCoroutinesVersion("1.6.0-RC2")
    ktlintVersion("0.43.2")
    detektVersion("1.19.0")
    jmhVersion("1.32")
    tests {
        junitJupiterVersion("5.8.2")
        jacocoVersion("0.8.7")
        useKotest("5.0.2")
        useMockk("1.12.1")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

dependencies {
    implementation("org.apache.calcite:calcite-core:1.29.0-SNAPSHOT")
    compileOnly("org.immutables:value:2.8.8")

    implementation("com.graphql-java:graphql-java:17.3")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}