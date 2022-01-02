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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xabi-stability=stable",
            "-Xassertions=jvm",
            "-Xenhance-type-parameter-types-to-def-not-null",
            "-Xjsr305=strict",
            "-Xjvm-default=all",
            "-Xlambdas=indy",
            "-Xparallel-backend-threads=0",
            "-Xsam-conversions=indy",
            "-Xstrict-java-nullability-assertions",
            "-Xtype-enhancement-improvements-strict-mode",
            "-Xuse-fast-jar-file-system",
            "-Xeffect-system",
            "-Xenable-builder-inference",
            "-Xexpect-actual-linker",
            "-Xextended-compiler-checks",
            // "-Xinference-compatibility",
            "-Xinline-classes",
            "-Xnew-inference",
            "-Xpolymorphic-signature",
            "-Xread-deserialized-contracts",
            "-Xself-upper-bound-inference",
            "-Xunrestricted-builder-inference",
            "-Xuse-fir",
            "-Xuse-fir-extended-checkers",
        )
    }
}

object Versions {
    const val calcite = "1.29.0"
}

dependencies {
    implementation("com.graphql-java:graphql-java:17.3")
    implementation("org.apache.calcite:calcite-core:${Versions.calcite}")
    implementation("org.apache.calcite:calcite-testkit:${Versions.calcite}")
    // Use 2.4.1 to remain compatible with Calcite
    implementation("org.hsqldb:hsqldb:2.4.1")

    compileOnly("org.immutables:value:2.8.8")
}
