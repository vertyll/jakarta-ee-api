plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.redisson)
    compileOnly(libs.microprofile.config.api)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.redisson)
}
