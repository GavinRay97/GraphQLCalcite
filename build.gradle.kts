plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("org.jetbrains.qodana") version "0.1.12"
    id("org.jetbrains.kotlinx.kover") version "0.4.4"

    // https://github.com/mrkuz/kradle
    id("net.bitsandbobs.kradle") version "2.0.1"
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
    kotlinJvmApplication {
        jvm {
            application {
                mainClass("ExampleKotlin")
            }

            /*
            // These are the defaults anyway
            targetJvm("17")
            kotlin {
                useCoroutines("1.6.0")
                lint {
                    ktlintVersion("0.43.2")
                }
                codeAnalysis {
                    detektVersion("1.19.0")
                }
                test {
                    useKotest("5.0.3")
                    useMockk("1.12.2")
                }
            }
            benchmark {
                jmhVersion("1.32")
            }
            test {
                withJunitJupiter("5.8.2")
                withJacoco("0.8.7")
            }
            */
        }
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
