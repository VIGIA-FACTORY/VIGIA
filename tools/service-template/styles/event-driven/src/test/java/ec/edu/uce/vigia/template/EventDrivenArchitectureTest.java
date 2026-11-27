package ec.edu.uce.vigia.template;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Fitness functions of ADR-037 v1.3 for the event-driven consumer style. */
@AnalyzeClasses(packagesOf = TemplateApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class EventDrivenArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "..listener..", "..handler..", "..publisher..")
            .allowEmptyShould(true)
            .because("the domain depends on nothing but itself (DP-05)");

    @ArchTest
    static final ArchRule handlersDoNotDependOnListeners = noClasses()
            .that().resideInAPackage("..handler..")
            .should().dependOnClassesThat().resideInAPackage("..listener..")
            .allowEmptyShould(true)
            .because("listeners call handlers, never the other way round");
}
