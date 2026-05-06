import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    war
    pmd
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.nullaway) apply false
    alias(libs.plugins.spotbugs) apply false
}

group = "com.vertyll"
version = "1.0-SNAPSHOT"
description = "Jakarta EE - base API"

extra["author"] = "Mikołaj Gawron"
extra["email"] = "gawrmiko@gmail.com"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("java")
        plugin("war")
        plugin("pmd")
        plugin("com.diffplug.spotless")
        plugin("net.ltgt.errorprone")
        plugin("net.ltgt.nullaway")
        plugin("com.github.spotbugs")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    dependencies {
        // Compile Only
        compileOnly(rootProject.libs.jspecify)

        // SpotBugs Annotations
        compileOnly(rootProject.libs.spotbugs.annotations)

        // Annotation Processor
        annotationProcessor(rootProject.libs.guava.beta.checker)

        // Error Prone
        add("errorprone", rootProject.libs.errorprone.core)
        add("errorprone", rootProject.libs.nullaway)

        // SpotBugs
        add("spotbugsPlugins", rootProject.libs.findsecbugs)

        // Test Compile Only
        testCompileOnly(rootProject.libs.jspecify)
        testCompileOnly(rootProject.libs.spotbugs.annotations)

        // Test Runtime Only
        testRuntimeOnly(rootProject.libs.junit.engine)
    }

    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        ignoreFailures.set(false)
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
        showProgress.set(true)

        excludeFilter.set(rootProject.file("config/spotbugs/exclude-filter.xml"))
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

        options.errorprone {
            enabled.set(true)

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

    tasks.withType<Test> {
        useJUnitPlatform()

        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

        testLogging {
            events("standardOut", "standardError")
            showStandardStreams = false
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
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

        addTestListener(object : TestListener {
            override fun beforeSuite(desc: TestDescriptor) {}
            override fun beforeTest(desc: TestDescriptor) {}

            override fun afterTest(desc: TestDescriptor, result: TestResult) {
                val indicator = when (result.resultType) {
                    TestResult.ResultType.SUCCESS -> "$ansiGreen$checkMark$ansiReset"
                    TestResult.ResultType.FAILURE -> "$ansiRed$crossMark$ansiReset"
                    TestResult.ResultType.SKIPPED -> "$ansiYellow$skipMark$ansiReset"
                    else -> "?"
                }
                val duration = result.endTime - result.startTime
                println("  $indicator ${desc.className} > ${desc.name} $ansiCyan(${duration}ms)$ansiReset")
            }

            override fun afterSuite(desc: TestDescriptor, result: TestResult) {
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
            }
        })
    }

//    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
//        java {
//            target("src/main/java/**/*.java", "src/test/java/**/*.java")
//            targetExclude("**/build/generated/**/*.java", "**/*Impl.java")
//
//            googleJavaFormat(rootProject.libs.versions.google.java.format.get()).aosp()
//
//            removeUnusedImports()
//            importOrder("java", "javax", "org", "com", "lombok", "com.vertyll")
//
//            trimTrailingWhitespace()
//            endWithNewline()
//
//            toggleOffOn()
//        }
//
//        format("gradle") {
//            target("*.gradle.kts", "**/*.gradle.kts")
//            trimTrailingWhitespace()
//            leadingTabsToSpaces(4)
//            endWithNewline()
//        }
//    }

    pmd {
        isConsoleOutput = true
        toolVersion = rootProject.libs.versions.pmd.get()
        ruleSets = listOf()
        ruleSetFiles = files(rootProject.file("config/pmd/pmd-main-ruleset.xml"))
        isIgnoreFailures = false
    }

    tasks.withType<Pmd> {
        if (name == "pmdTest") {
            ruleSetFiles = files(rootProject.file("config/pmd/pmd-test-ruleset.xml"))
        }
    }
}

tasks.register<TestReport>("testReport") {
    group = "verification"
    description = "Generate aggregated test report for all modules"
    destinationDirectory.set(layout.buildDirectory.dir("reports/all-tests"))

    testResults.from(subprojects.map { it.tasks.withType<Test>() })

    doLast {
        val reportFile = destinationDirectory.get().file("index.html").asFile
        println("\nAggregated test report generated:")
        println("   file://${reportFile.absolutePath}\n")

        if (project.hasProperty("openReport")) {
            val os = System.getProperty("os.name").lowercase()
            try {
                when {
                    os.contains("mac") -> {
                        Runtime.getRuntime().exec(arrayOf("open", reportFile.absolutePath))
                    }

                    os.contains("nix") || os.contains("nux") -> {
                        Runtime.getRuntime().exec(arrayOf("xdg-open", reportFile.absolutePath))
                    }

                    os.contains("win") -> {
                        Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", reportFile.absolutePath))
                    }
                }
                println("Opening report in browser...\n")
            } catch (e: Exception) {
                println("Could not open browser automatically: ${e.message}\n")
            }
        } else {
            println("Add -PopenReport to open in browser automatically\n")
        }
    }
}

tasks.register("testAll") {
    group = "verification"
    description = "Run all tests in all modules and generate aggregated report"

    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    finalizedBy("testReport")
}

tasks.register("spotbugsAll") {
    group = "verification"
    description = "Run SpotBugs analysis on all modules"

    dependsOn(subprojects.map { it.tasks.withType<com.github.spotbugs.snom.SpotBugsTask>() })

    doLast {
        val ansiReset = "\u001B[0m"
        val ansiGreen = "\u001B[32m"
        val ansiRed = "\u001B[31m"
        val ansiBold = "\u001B[1m"

        println("\n$ansiBold═══════════════════════════════════════════════════════════════$ansiReset")
        println("$ansiBold                   SPOTBUGS SUMMARY                        $ansiReset")
        println("$ansiBold═══════════════════════════════════════════════════════════════$ansiReset\n")

        var totalBugs = 0
        val moduleResults = mutableListOf<Triple<String, Int, String>>()

        subprojects.forEach { project ->
            val reportDir = project.layout.buildDirectory.get().dir("reports/spotbugs").asFile
            if (reportDir.exists()) {
                reportDir.listFiles()?.filter { it.extension == "xml" }?.forEach { xmlFile ->
                    val xml = xmlFile.readText()
                    val bugCount = xml.substringAfter("<BugInstance", "").let {
                        if (it.isEmpty()) 0 else xml.split("<BugInstance").size - 1
                    }
                    totalBugs += bugCount

                    val htmlReport = xmlFile.resolveSibling(xmlFile.nameWithoutExtension + ".html")
                    moduleResults.add(Triple(project.name, bugCount, htmlReport.absolutePath))
                }
            }
        }

        moduleResults.forEach { (name, count, reportPath) ->
            val status = if (count == 0) "${ansiGreen}✓$ansiReset" else "${ansiRed}✗$ansiReset"
            println("  $status $name: $count bug(s)")
            if (count > 0) {
                println("     file://$reportPath")
            }
        }

        println("\n  ${ansiBold}Total bugs found: $totalBugs$ansiReset")

        if (totalBugs == 0) {
            println("  ${ansiGreen}All modules passed SpotBugs analysis!$ansiReset")
        }

        println("\n$ansiBold═══════════════════════════════════════════════════════════════$ansiReset\n")
    }
}