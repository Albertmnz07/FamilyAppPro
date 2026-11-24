package albertmnz07.familyapp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Enforces the hexagonal layer and framework-purity rules recorded in ADR-0010. */
@AnalyzeClasses(
        packages = "albertmnz07.familyapp",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "albertmnz07.familyapp.domain..";
    private static final String APPLICATION = "albertmnz07.familyapp.application..";
    private static final String INFRASTRUCTURE = "albertmnz07.familyapp.infrastructure..";
    private static final String LEGACY = "albertmnz07.familyapp.legacy..";

    @ArchTest
    static final ArchRule domainDependsOnlyOnDomainAndStandardJava =
            classes()
                    .that()
                    .resideInAPackage(DOMAIN)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(DOMAIN, "java..");

    @ArchTest
    static final ArchRule applicationDependsOnlyOnApplicationDomainAndStandardJava =
            classes()
                    .that()
                    .resideInAPackage(APPLICATION)
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(APPLICATION, DOMAIN, "java..");

    @ArchTest
    static final ArchRule coreLayersNeverDependOnInfrastructureFrameworksOrLegacy =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAIN, APPLICATION)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            INFRASTRUCTURE,
                            LEGACY,
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation..");

    @ArchTest
    static final ArchRule noProductionClassesRemainInOrDependOnLegacy =
            noClasses()
                    .should()
                    .resideInAPackage(LEGACY)
                    .orShould()
                    .dependOnClassesThat()
                    .resideInAPackage(LEGACY);

    @ArchTest
    static void noClassesInMainOrTestSourcesRemainInOrDependOnLegacy(
            JavaClasses productionClasses) {
        JavaClasses allClasses = new ClassFileImporter().importPackages("albertmnz07.familyapp");
        noClasses()
                .should()
                .resideInAPackage(LEGACY)
                .orShould()
                .dependOnClassesThat()
                .resideInAPackage(LEGACY)
                .check(allClasses);
    }
}
