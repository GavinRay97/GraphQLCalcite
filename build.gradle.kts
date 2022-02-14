plugins {
    `java`

    // https://github.com/mrkuz/kradle
    id("net.bitsandbobs.kradle") version "2.1.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repository.apache.org/content/repositories/snapshots") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

kradle {
    // Default values and unused features are commented out, but kept for reference
    jvm {
        application {
            mainClass("com.example.demo.App")
        }

        targetJvm("17")
        java {
            previewFeatures(true)
            /*
            lint {
                checkstyle {
                    version("9.2.1")
                    configFile("checkstyle.xml")
                }
            }
            */
            codeAnalysis {
                pmd {
                    version("6.41.0")
                    ruleSets {
                        bestPractices(true)
                        codeStyle(false)
                        design(true)
                        documentation(false)
                        errorProne(true)
                        multithreading(true)
                        performance(true)
                        security(true)
                    }
                }
                spotBugs {
                    // version("4.5.3")
                    useFbContrib(/* "7.4.7" */)
                    useFindSecBugs(/* "1.11.0" */)
                }
            }
        }

        codeAnalysis {
            // ignoreFailures(false)
        }
        developmentMode.enable()
        test.disable()

        benchmark {
            // jmhVersion("1.34")
        }

        packaging {
            uberJar {
                // minimize(false)
            }
        }

        docker {
            // baseImage("bellsoft/liberica-openjdk-alpine:17")
            withJvmKill(/* "1.16.0" */)
            // startupScript(false)
            // ports(...)
            // jvmOpts("...")
        }

        // documentation.enable()
    }
}

object Versions {
    const val calcite = "1.29.0"
    const val graphqlJava = "17.3"
    const val immutables = "2.8.8"
    const val testcontainers = "1.16.2"
}

dependencies {
    implementation("com.graphql-java:graphql-java:${Versions.graphqlJava}")

    implementation("org.apache.calcite:calcite-core:${Versions.calcite}")
    implementation("org.apache.calcite:calcite-testkit:${Versions.calcite}")
    implementation("org.apache.calcite:calcite-csv:${Versions.calcite}")

    // Use 2.4.1 to remain compatible with Calcite
    implementation("org.hsqldb:hsqldb:2.4.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("com.microsoft.sqlserver:mssql-jdbc:9.4.0.jre8")

    // SchemaCrawler
    compileOnly("org.immutables:value:${Versions.immutables}")

    testImplementation("org.slf4j:slf4j-simple:2.0.0-alpha6")
    testImplementation("org.testcontainers:junit-jupiter:${Versions.testcontainers}")
    implementation("org.testcontainers:testcontainers:${Versions.testcontainers}")
    implementation("org.testcontainers:postgresql:${Versions.testcontainers}")
    implementation("org.testcontainers:mysql:${Versions.testcontainers}")
}

tasks.test {
    useJUnitPlatform()
}
