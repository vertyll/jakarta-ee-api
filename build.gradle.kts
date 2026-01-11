import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    war
    pmd
    alias(libs.plugins.spotless)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
    alias(libs.plugins.spotbugs)
}

group = "com.vertyll"
version = "1.0-SNAPSHOT"
description = "Jakarta EE - base API"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    // Implementation dependencies
    implementation(libs.mongodb.driver.sync)
    implementation(libs.bundles.jjwt)
    implementation(libs.mapstruct)
    implementation(libs.guava)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    // Bean Validation
    implementation(libs.hibernate.validator)
    implementation(libs.expressly)

    // Compile-only dependencies
    compileOnly(libs.bundles.jakarta)
    compileOnly(libs.lombok)
    compileOnly(libs.jspecify)

    // SpotBugs Annotations
    compileOnly(libs.spotbugs.annotations)

    // Annotation processors
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.bundles.mapstruct.processors)
    annotationProcessor(libs.guava.beta.checker)

    // Error Prone & NullAway
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)

    // SpotBugs
    add("spotbugsPlugins", libs.findsecbugs)

    // Test compile-only dependencies
    testCompileOnly(libs.lombok)

    // Test Compile Only
    testCompileOnly(libs.jspecify)
    testCompileOnly(libs.spotbugs.annotations)

    // Test implementation dependencies
    testImplementation(libs.bundles.testing)

    // Test annotation processors
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.bundles.mapstruct.processors)

    // Test runtime-only dependencies
    testRuntimeOnly(libs.junit.engine)
}

configure<com.github.spotbugs.snom.SpotBugsExtension> {
    ignoreFailures.set(false)
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    showProgress.set(true)

    excludeFilter.set(file("config/spotbugs/exclude-filter.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    val projectName = project.name
    val taskName = name
    val buildDir = project.layout.buildDirectory.get()

    reports.maybeCreate("html").apply {
        required.set(true)
        outputLocation.set(file("${buildDir}/reports/spotbugs/${projectName}-${taskName}.html"))
        setStylesheet("fancy-hist.xsl")
    }

    reports.maybeCreate("xml").apply {
        required.set(true)
        outputLocation.set(file("${buildDir}/reports/spotbugs/${projectName}-${taskName}.xml"))
    }

    doLast {
        val ansiReset = "\u001B[0m"
        val ansiGreen = "\u001B[32m"
        val ansiRed = "\u001B[31m"
        val ansiBold = "\u001B[1m"

        val xmlReport = reports.maybeCreate("xml").outputLocation.get().asFile
        val htmlReport = reports.maybeCreate("html").outputLocation.get().asFile

        if (xmlReport.exists()) {
            val xml = xmlReport.readText()
            val bugCount = xml.substringAfter("<BugInstance", "").let {
                if (it.isEmpty()) 0 else xml.split("<BugInstance").size - 1
            }

            println("\n$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")
            println("$ansiBold  SpotBugs: $projectName - $taskName")
            println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")

            if (bugCount == 0) {
                println("  ${ansiGreen}✓ No bugs found!$ansiReset")
            } else {
                println("  ${ansiRed}✗ Found $bugCount bug(s)$ansiReset")

                val categories = mutableMapOf<String, Int>()
                xml.split("<BugInstance").drop(1).forEach { bugXml ->
                    val category = bugXml.substringAfter("category=\"", "").substringBefore("\"", "UNKNOWN")
                    categories[category] = categories.getOrDefault(category, 0) + 1
                }

                println("\n  Categories:")
                categories.forEach { (category, count) ->
                    println("    • $category: $count")
                }

                if (htmlReport.exists()) {
                    println("\n  Detailed report: file://${htmlReport.absolutePath}")
                }
            }
            println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset\n")
        }
    }
}

tasks.named("check") {
    dependsOn("spotbugsMain")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")

    options.encoding = "UTF-8"

    options.errorprone {
        isEnabled.set(true)

        check("NullAway", net.ltgt.gradle.errorprone.CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:CustomContractAnnotations", "org.springframework.lang.Contract")
        option("NullAway:JSpecifyMode", "true")

        option("NullAway:ExcludedFieldAnnotations", "lombok.Generated")
        option("NullAway:TreatGeneratedAsUnannotated", "true")

        option("NullAway:AcknowledgeRestrictiveAnnotations", "true")
        option("NullAway:CheckOptionalEmptiness", "true")
        option("NullAway:HandleTestAssertionLibraries", "true")

        excludedPaths.set(".*/build/generated/.*")
    }
}

pmd {
    isConsoleOutput = true
    toolVersion = libs.versions.pmd.get()
    ruleSets = listOf()
    ruleSetFiles = files(file("config/pmd/pmd-main-ruleset.xml"))
    isIgnoreFailures = false
}

tasks.withType<Pmd> {
    if (name == "pmdTest") {
        ruleSetFiles = files(file("config/pmd/pmd-test-ruleset.xml"))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = false
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        displayGranularity = 2
    }

    val ansiReset = "\u001B[0m"
    val ansiGreen = "\u001B[32m"
    val ansiRed = "\u001B[31m"
    val ansiYellow = "\u001B[33m"
    val ansiCyan = "\u001B[36m"
    val ansiBold = "\u001B[1m"

    val checkMark = "✓"
    val crossMark = "✗"
    val skipMark = "⊘"

    afterTest(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        val indicator = when (result.resultType) {
            TestResult.ResultType.SUCCESS -> "$ansiGreen$checkMark$ansiReset"
            TestResult.ResultType.FAILURE -> "$ansiRed$crossMark$ansiReset"
            TestResult.ResultType.SKIPPED -> "$ansiYellow$skipMark$ansiReset"
            else -> "?"
        }
        val duration = result.endTime - result.startTime
        println("  $indicator ${desc.className} > ${desc.name} $ansiCyan(${duration}ms)$ansiReset")
    }))

    afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        if (desc.parent == null) {
            val total = result.testCount
            val passed = result.successfulTestCount
            val failed = result.failedTestCount
            val skipped = result.skippedTestCount
            val duration = result.endTime - result.startTime

            println()
            println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")
            println("$ansiBold                        TEST RESULTS                        $ansiReset")
            println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")
            println()
            println("  Total:   $ansiBold$total$ansiReset tests")
            println("  Passed:  $ansiGreen$ansiBold$passed$ansiReset $ansiGreen$checkMark$ansiReset")
            println("  Failed:  $ansiRed$ansiBold$failed$ansiReset ${if (failed > 0) "$ansiRed$crossMark$ansiReset" else ""}")
            println("  Skipped: $ansiYellow$ansiBold$skipped$ansiReset ${if (skipped > 0) "$ansiYellow$skipMark$ansiReset" else ""}")
            println()
            println("  Duration: $ansiCyan${duration}ms$ansiReset")
            println()

            val statusColor = when (result.resultType) {
                TestResult.ResultType.SUCCESS -> ansiGreen
                TestResult.ResultType.FAILURE -> ansiRed
                else -> ansiYellow
            }
            println("  Status: $statusColor$ansiBold${result.resultType}$ansiReset")
            println()
            println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")
            println()
        }
    }))
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        target("src/main/java/**/*.java", "src/test/java/**/*.java")
        targetExclude("**/build/generated/**/*.java", "**/*Impl.java")

        googleJavaFormat(libs.versions.google.java.format.get()).aosp()

        removeUnusedImports()
        importOrder("java", "javax", "org", "com", "lombok", "com.vertyll")

        trimTrailingWhitespace()
        endWithNewline()

        toggleOffOn()
    }

    format("gradle") {
        target("*.gradle.kts", "**/*.gradle.kts")
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
}
