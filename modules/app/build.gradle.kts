plugins {
    java
    war
}

dependencies {
    // Implementation - Internal Modules
    implementation(project(":modules:common"))
    implementation(project(":modules:config"))

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

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<War>("war") {
    archiveFileName.set("${project.name}.war")

    // Optional: exclude certain files if needed
    // exclude("WEB-INF/lib/some-excluded-lib*.jar")
}

tasks.named<Jar>("jar") {
    enabled = false
}
