plugins {
    id("java-library")
}

dependencies {
    implementation(libs.mongodb.driver.sync)
    implementation(libs.mapstruct)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    implementation(libs.hibernate.validator)
    implementation(libs.expressly)

    compileOnly(libs.bundles.jakarta)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)
    annotationProcessor(libs.bundles.mapstruct.processors)
    annotationProcessor(libs.guava.beta.checker)

    testCompileOnly(libs.lombok)

    testImplementation(libs.bundles.testing)

    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.bundles.mapstruct.processors)
}
