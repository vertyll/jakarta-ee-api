plugins {
    war
    alias(libs.plugins.liberty)
}

val sessionStore = configurations.create("sessionStore")

dependencies {
    // The WAR assembles the modules; it holds only the JAX-RS application, the health check and Liberty's setup.
    implementation(project(":modules:common"))
    implementation(project(":modules:auth:auth-infrastructure"))
    implementation(project(":modules:user:user-infrastructure"))

    runtimeOnly(libs.logback.classic)

    compileOnly(libs.jakartaee.web.api)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.archunit.junit5)

    sessionStore(libs.redisson)
    sessionStore(project(":modules:session-store"))
    if (System.getProperty("os.name").startsWith("Mac")) {
        val arch = if (System.getProperty("os.arch") == "aarch64") "osx-aarch_64" else "osx-x86_64"
        sessionStore(variantOf(libs.netty.resolver.dns.native.macos) { classifier(arch) })
    }

    libertyRuntime(libs.openliberty.runtime)
}

val copySessionStore =
    tasks.register<Copy>("copySessionStore") {
        group = "liberty"
        description = "Copies the Redisson session store into the Liberty shared resources"
        from(sessionStore)
        into(layout.buildDirectory.dir("wlp/usr/shared/resources/redisson"))
    }

listOf("libertyCreate", "deploy", "libertyRun", "libertyStart", "libertyDev", "libertyPackage").forEach { name ->
    tasks.matching { it.name == name }.configureEach { dependsOn(copySessionStore) }
}

tasks.war {
    archiveFileName.set("${rootProject.name}.war")
}

tasks.jar {
    enabled = false
}

liberty {
    server.name = rootProject.name
    server.packageLiberty.packageName = rootProject.name
    server.packageLiberty.include = "runnable"
}

// The local settings (the containers' addresses, the local realm's client secret, cookies without TLS) live in
// src/main/liberty/local, outside the config directory every server is built from, so the runnable jar ships none of
// them and a deployment sets its own environment. Only a server started from Gradle on a developer's machine, by
// libertyDev, libertyRun or libertyStart, reads them.
val localServerEnv = layout.projectDirectory.file("src/main/liberty/local/server.env").asFile
val serverEnv = layout.buildDirectory.file("wlp/usr/servers/${rootProject.name}/server.env")

gradle.taskGraph.whenReady {
    if (allTasks.none { it.name == "libertyPackage" }) {
        liberty.server.serverEnvFile = localServerEnv
    }
}

// A server a developer started earlier still holds the local settings; they are taken out before it is packaged,
// leaving what Liberty generated itself.
tasks.matching { it.name == "libertyPackage" }.configureEach {
    doFirst {
        val packaged = serverEnv.get().asFile
        if (packaged.exists() && localServerEnv.exists()) {
            val localNames =
                localServerEnv
                    .readLines()
                    .map {
                        it
                            .substringBefore(
                                '=',
                            ).trim()
                    }.filter { it.isNotEmpty() }
            val kept = packaged.readLines().filterNot { it.substringBefore('=').trim() in localNames }
            packaged.writeText(kept.joinToString(separator = "\n", postfix = if (kept.isEmpty()) "" else "\n"))
        }
    }
}
