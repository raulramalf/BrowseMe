package io.github.raulramalf.browseme.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Guarda las fronteras de la arquitectura hexagonal. Si este test falla, el
 * cambio que lo rompe está metiendo un framework o una capa externa donde no debe.
 */
@AnalyzeClasses(packages = "io.github.raulramalf.browseme", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "io.github.raulramalf.browseme.domain..";
    private static final String APPLICATION = "io.github.raulramalf.browseme.application..";
    private static final String INFRASTRUCTURE = "io.github.raulramalf.browseme.infrastructure..";

    private static final String[] FRAMEWORKS = {
        "org.springframework..",
        "jakarta.persistence..",
        "org.hibernate..",
        "org.jsoup..",
        "okhttp3..",
        "okio..",
        "tools.jackson..",
        "com.fasterxml.jackson..",
        "io.github.bucket4j.."
    };

    /** Regla más estricta que una lista negra: el dominio solo ve el JDK y a sí mismo. */
    @ArchTest
    static final ArchRule domain_depends_only_on_jdk_and_itself = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN);

    /** Explícita para que el mensaje de error nombre el framework concreto. */
    @ArchTest
    static final ArchRule domain_does_not_use_frameworks = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS);

    @ArchTest
    static final ArchRule application_does_not_use_frameworks_or_infrastructure = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
            .orShould().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            // La capa de aplicación aún no existe; la regla queda armada para cuando llegue.
            .allowEmptyShould(true);
}
