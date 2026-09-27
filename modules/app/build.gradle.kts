plugins {
    war
    alias(libs.plugins.liberty)
}

dependencies {
    implementation(project(":modules:common"))
    implementation(project(":modules:config"))

    runtimeOnly(libs.logback.classic)

    compileOnly(libs.jakartaee.web.api)

    testImplementation(libs.bundles.testing)

    libertyRuntime(libs.openliberty.webprofile)
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
