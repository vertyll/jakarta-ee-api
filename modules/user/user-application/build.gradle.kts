// The use case of mirroring an account, and its port. Plain Java, no Jakarta EE.
plugins {
    `java-library`
}

dependencies {
    api(project(":modules:user:user-domain"))

    testImplementation(libs.bundles.testing)
}
