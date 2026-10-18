// The use cases of signing in, refreshing and signing out, and the ports they need. Plain Java, no Jakarta EE.
plugins {
    `java-library`
}

dependencies {
    api(project(":modules:auth:auth-domain"))
    api(project(":modules:auth:auth-contracts"))

    implementation(libs.slf4j.api)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.bundles.testing)
}
