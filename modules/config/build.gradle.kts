plugins {
    id("java-library")
}

dependencies {
    // API - Internal Modules
//    api(project(":modules:example"))

    // Implementation dependencies
    implementation(libs.mongodb.driver.sync)
    implementation(libs.mapstruct)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)

    // Bean Validation
    implementation(libs.hibernate.validator)
    implementation(libs.expressly)

    // Compile-only dependencies
    compileOnly(libs.bundles.jakarta)
    compileOnly(libs.lombok)

    // Annotation processors
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.bundles.mapstruct.processors)
    annotationProcessor(libs.guava.beta.checker)

    // Test compile-only dependencies
    testCompileOnly(libs.lombok)

    // Test implementation dependencies
    testImplementation(libs.bundles.testing)

    // Test annotation processors
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.bundles.mapstruct.processors)
}
