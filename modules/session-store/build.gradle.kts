plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.redisson)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.redisson)
}
