plugins {
    war
    alias(libs.plugins.liberty)
}

val sessionStore: Configuration by configurations.creating

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:config"))
    implementation(libs.mongodb.driver.sync)
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.classic)

    compileOnly(libs.jakartaee.web.api)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.jakartaee.web.api)
    testRuntimeOnly(libs.yasson)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    sessionStore(libs.redisson)

    libertyRuntime(libs.openliberty.runtime)
}

val copySessionStore by tasks.registering(Copy::class) {
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
