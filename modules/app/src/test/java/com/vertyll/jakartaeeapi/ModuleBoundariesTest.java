package com.vertyll.jakartaeeapi;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The modular monolith's boundaries and each module's hexagon. Gradle already keeps a module from compiling against
 * another's inside; these rules also hold the layers inside each module, and keep frameworks out of the domain and the
 * application.
 */
@AnalyzeClasses(packages = "com.vertyll.jakartaeeapi", importOptions = ImportOption.DoNotIncludeTests.class)
final class ModuleBoundariesTest {
    private static final String BASE = "com.vertyll.jakartaeeapi";

    @ArchTest
    static final ArchRule MODULES_ARE_FREE_OF_CYCLES =
            slices().matching(BASE + ".(auth|user).(**)").namingSlices("$1").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule USER_REACHES_AUTH_ONLY_THROUGH_ITS_CONTRACTS = noClasses().that()
        .resideInAPackage(BASE + ".user..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(BASE + ".auth.domain..", BASE + ".auth.application..", BASE + ".auth.infrastructure..")
        .because("a module is reached only through its contracts");

    @ArchTest
    static final ArchRule AUTH_KNOWS_NO_OTHER_MODULE = noClasses().that()
        .resideInAPackage(BASE + ".auth..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(BASE + ".user..")
        .because("modules that keep accounts depend on authentication, never the reverse");

    @ArchTest
    static final ArchRule THE_DOMAIN_KNOWS_NO_OUTER_LAYER_AND_NO_FRAMEWORK = noClasses().that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..",
            "..infrastructure..",
            "jakarta..",
            "org.redisson..",
            "com.mongodb..",
            "org.bson..",
            "com.nimbusds.."
        )
        .because("dependencies point inwards, and the domain is plain Java");

    @ArchTest
    static final ArchRule THE_APPLICATION_KNOWS_NO_ADAPTER_AND_NO_FRAMEWORK = noClasses().that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..infrastructure..",
            "jakarta..",
            "org.redisson..",
            "com.mongodb..",
            "org.bson..",
            "com.nimbusds.."
        )
        .because("adapters implement the application's ports, and the infrastructure wires it");

    @ArchTest
    static final ArchRule THE_CONTRACTS_ARE_PLAIN_JAVA = noClasses().that()
        .resideInAPackage(BASE + ".auth.contracts..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            BASE + ".auth.domain..",
            BASE + ".auth.application..",
            BASE + ".auth.infrastructure..",
            "jakarta.."
        )
        .because("the published contract stays independent of how authentication works inside");

    private ModuleBoundariesTest() {
    }
}
