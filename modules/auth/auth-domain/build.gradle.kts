// The sign-in's own model: identities, sessions, the PKCE transaction and the ways a sign-in fails. Plain Java.
plugins {
    `java-library`
}

dependencies {
    api(project(":modules:common"))

    testImplementation(libs.bundles.testing)
}
