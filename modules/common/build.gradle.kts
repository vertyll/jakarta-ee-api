plugins {
    `java-library`
}

dependencies {
    implementation(libs.slf4j.api)

    compileOnly(libs.jakartaee.web.api)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)

    testCompileOnly(libs.lombok)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.jakartaee.web.api)
    testRuntimeOnly(libs.resteasy.core)

    testAnnotationProcessor(libs.lombok)
}
