plugins {
    war
    alias(libs.plugins.liberty)
}

val sessionStore = configurations.create("sessionStore")

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:config"))
    implementation(libs.mongodb.driver.sync)
    implementation(libs.nimbus.jose.jwt)
    compileOnly(libs.redisson)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.classic)

    compileOnly(libs.jakartaee.web.api)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.jakartaee.web.api)
    testImplementation(libs.testcontainers)
    testImplementation(libs.redisson)
    testRuntimeOnly(libs.yasson)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    sessionStore(libs.redisson)
    sessionStore(project(":modules:session-store"))
    if (System.getProperty("os.name").startsWith("Mac")) {
        val arch = if (System.getProperty("os.arch") == "aarch64") "osx-aarch_64" else "osx-x86_64"
        sessionStore(variantOf(libs.netty.resolver.dns.native.macos) { classifier(arch) })
    }

    libertyRuntime(libs.openliberty.runtime)
}

val copySessionStore = tasks.register<Copy>("copySessionStore") {
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
