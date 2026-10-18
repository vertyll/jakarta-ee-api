// The adapters of authentication: the JAX-RS resource and filters, Keycloak's token endpoint and keys, and Redis.
plugins {
    `java-library`
}

dependencies {
    api(project(":modules:auth:auth-application"))
    implementation(project(":modules:common"))

    implementation(libs.nimbus.jose.jwt)
    implementation(libs.slf4j.api)
    compileOnly(libs.redisson)
    compileOnly(libs.jakartaee.web.api)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.jakartaee.web.api)
    testImplementation(libs.testcontainers)
    testImplementation(libs.redisson)
    testRuntimeOnly(libs.yasson)
    // The JAX-RS implementation Liberty runs, so a test can build the responses a resource or a filter returns.
    testRuntimeOnly(libs.resteasy.core)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
