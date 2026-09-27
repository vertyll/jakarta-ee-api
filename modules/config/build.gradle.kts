plugins {
    `java-library`
}

dependencies {
    implementation(libs.mongodb.driver.sync)

    compileOnly(libs.jakartaee.web.api)

    testImplementation(libs.bundles.testing)
}
