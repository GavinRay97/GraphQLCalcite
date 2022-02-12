plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.0-dev-1582"
    // https://github.com/mrkuz/kradle
    id("net.bitsandbobs.kradle") version "2.2.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/") }
    maven { url = uri("https://repository.apache.org/content/repositories/snapshots") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

kradle {
    jvm {
        application {
            mainClass("…")
        }

        // dependencies.enable()
        // vulnerabilityScan.enable()
        lint.enable()
        codeAnalysis.enable()
        developmentMode.enable()

        test {
            prettyPrint(true)
            withIntegrationTests()
            withFunctionalTests()
            useTestcontainers()
            junitJupiter {
                version("5.8.2")
            }
        }
        codeCoverage.enable()
        benchmark.enable()
        packaging.enable()
        docker {
            withJvmKill()
        }
        // documentation.enable()
    }
}

object Versions {
    const val calcite = "1.29.0"
    const val graphqlJava = "17.3"
    const val immutables = "2.8.8"
    const val testcontainers = "1.16.2"
    const val strikt = "0.33.0"
    const val ktor = "2.0.0-beta-1"
    const val vertx = "4.2.4"
    const val kotlinxHtml = "0.7.3"
}

dependencies {
    // graphql-java
    implementation("com.graphql-java:graphql-java:${Versions.graphqlJava}")

    // Apache Calcite
    implementation("org.apache.calcite:calcite-core:${Versions.calcite}")
    implementation("org.apache.calcite:calcite-testkit:${Versions.calcite}")
    implementation("org.apache.calcite:calcite-csv:${Versions.calcite}")
    compileOnly("org.immutables:value:${Versions.immutables}")
    implementation("org.hsqldb:hsqldb:2.4.1")

    // Vert.x
    implementation(platform("io.vertx:vertx-stack-depchain:${Versions.vertx}"))
    implementation("io.vertx:vertx-jdbc-client")
    implementation("io.vertx:vertx-oracle-client")
    implementation("io.vertx:vertx-web-graphql")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-pg-client")
    implementation("io.vertx:vertx-mysql-client")
    implementation("io.vertx:vertx-mssql-client")
    implementation("io.vertx:vertx-lang-kotlin-coroutines")
    implementation("io.vertx:vertx-lang-kotlin")

    implementation(kotlin("script-runtime"))
    // Kotlinx HTML
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${Versions.kotlinxHtml}")
    implementation("org.jetbrains.kotlinx:kotlinx-html:${Versions.kotlinxHtml}")

    // Use H2 also because it has "JSON_ARRAYAGG"
    // JDBC Databases
    testImplementation("com.h2database:h2:2.1.210")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("com.microsoft.sqlserver:mssql-jdbc:9.4.0.jre8")

    testImplementation("org.slf4j:slf4j-simple:2.0.0-alpha6")

    // Testcontainers
    testImplementation("org.testcontainers:junit-jupiter:${Versions.testcontainers}")
    implementation("org.testcontainers:testcontainers:${Versions.testcontainers}")
    implementation("org.testcontainers:postgresql:${Versions.testcontainers}")
    implementation("org.testcontainers:mysql:${Versions.testcontainers}")

    // Strikt
    testImplementation(platform("io.strikt:strikt-bom:${Versions.strikt}"))
    testImplementation("io.strikt:strikt-jvm")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {

        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.Experimental",

            // enable, depend on jvm assertion settings
            "-Xassertions=jvm",

            // Reuse javac analysis and compile Java source files
            "-Xcompile-java",

            // Emit JVM type annotations in bytecode
            "-Xemit-jvm-type-annotations",

            // Enhance not null annotated type parameter's types to definitely not null types (@NotNull T => T & Any)
            "-Xenhance-type-parameter-types-to-def-not-null",

            // Compile multifile classes as a hierarchy of parts and facade
            "-Xmultifile-parts-inherit",

            // strict (experimental; treat as other supported nullability annotations)
            "-Xjsr305=strict",

            // Generate lambdas using `invokedynamic` with `LambdaMetafactory.metafactory`. Requires `"-jvm-target 1.8` or greater.",
            "-Xlambdas=indy",

            // When using the IR backend, run lowerings by file in N parallel threads. 0 means use a thread per processor core. Default value is 1
            "-Xbackend-threads=0",

            // Generate SAM conversions using `invokedynamic` with `LambdaMetafactory.metafactory`. Requires `"-jvm-target 1.8` or greater.",
            "-Xsam-conversions=indy",

            // Enable strict mode for some improvements in the type enhancement for loaded Java types based on nullability annotations,including freshly supported reading of the type use annotations from class files. See KT"-45671 for more details",
            "-Xtype-enhancement-improvements-strict-mode",

            // Use fast implementation on Jar FS. This may speed up compilation time, but currently it's an experimental mode
            "-Xuse-fast-jar-file-system",

            // Enable experimental context receivers
            "-Xcontext-receivers",

            // Enable experimental language feature: effect system
            "-Xeffect-system",

            // Enable additional compiler checks that might provide verbose diagnostic information for certain errors.
            "-Xextended-compiler-checks",

            // Enable incremental compilation
            "-Xenable-incremental-compilation",

            // Enable compatibility changes for generic type inference algorithm
            "-Xinference-compatibility",

            // Support inferring type arguments based on only self upper bounds of the corresponding type parameters
            "-Xself-upper-bound-inference",

            // Eliminate builder inference restrictions like allowance of returning type variables of a builder inference call
            "-Xunrestricted-builder-inference",

            // Compile using Front"-end IR. Warning: this feature is far from being production-ready",
            "-Xuse-fir",

            //  Use extended analysis mode based on Front"-end IR. Warning: this feature is far from being production-ready",
            "-Xuse-fir-extended-checkers"
        )
    }
}

