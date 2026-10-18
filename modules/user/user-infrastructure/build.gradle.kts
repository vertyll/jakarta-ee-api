// The adapters of the user module: MongoDB, the `/users` resource, and the port `auth` calls at every sign-in.
// Of `auth` it sees only its API.
plugins {
    `java-library`
}

dependencies {
    api(project(":modules:user:user-application"))
    implementation(project(":modules:auth:auth-api"))

    implementation(libs.mongodb.driver.sync)
    compileOnly(libs.jakartaee.web.api)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.jakartaee.web.api)
    testImplementation(libs.testcontainers)
}
