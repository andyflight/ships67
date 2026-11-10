package com.games.shipmaster;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/** The clean architecture rules (design D1), reusable against fixtures. */
final class ArchitectureRules {

    private ArchitectureRules() {
    }

    /** Domain: plain Java + Lombok + JPA annotations (the agreed exception) + other domain types. */
    static final ArchRule DOMAIN_IS_PURE = classes().that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..", "lombok..", "jakarta.persistence..", "..domain..", "")
            .because("domain must not depend on Spring, Jackson, web or outer layers");

    /** Application: domain, other application layers, and only Spring's stereotype/transaction annotations. */
    static final ArchRule APPLICATION_DEPENDS_INWARD = classes().that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..", "lombok..", "..domain..", "..application..",
                    "org.springframework.stereotype..", "org.springframework.transaction..", "")
            .because("use cases must not know about infrastructure or the web");

    static final ArchRule NOTHING_DEPENDS_ON_API = noClasses().that().resideOutsideOfPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..api..")
            .because("api is the outermost layer");

    static final ArchRule API_USES_APPLICATION_ONLY = noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat(resideInAPackage("..infrastructure..")
                    .or(resideInAPackage("..domain..").and(not(resideInAPackage("..common.domain..")))))
            .because("controllers talk to use cases, never to entities, repositories or infrastructure");
}
