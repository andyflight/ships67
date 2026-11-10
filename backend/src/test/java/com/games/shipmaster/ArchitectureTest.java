package com.games.shipmaster;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.games.shipmaster", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPure = ArchitectureRules.DOMAIN_IS_PURE;

    @ArchTest
    static final ArchRule applicationDependsInward = ArchitectureRules.APPLICATION_DEPENDS_INWARD;

    @ArchTest
    static final ArchRule nothingDependsOnApi = ArchitectureRules.NOTHING_DEPENDS_ON_API;

    @ArchTest
    static final ArchRule apiUsesApplicationOnly = ArchitectureRules.API_USES_APPLICATION_ONLY;

    /** Guards the guard: each rule must reject the deliberately broken fixture classes. */
    @Test
    void rulesRejectViolations() {
        JavaClasses bad = new ClassFileImporter().importPackages("com.games.shipmaster.archfixture");
        assertThatThrownBy(() -> ArchitectureRules.DOMAIN_IS_PURE.check(bad)).isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> ArchitectureRules.NOTHING_DEPENDS_ON_API.check(bad)).isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> ArchitectureRules.API_USES_APPLICATION_ONLY.check(bad)).isInstanceOf(AssertionError.class);
    }
}
