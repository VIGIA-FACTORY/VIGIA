package ec.edu.uce.vigia.template;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Fitness functions of ADR-037 and 03.08.05 for the hexagonal layout. */
@AnalyzeClasses(packagesOf = TemplateApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final int MAX_PORT_METHODS = 5;

    @ArchTest
    static final ArchRule domainIsFrameworkAndAdapterFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "org.springframework..")
            .allowEmptyShould(true)
            .because("the domain depends on nothing but itself (DP-05)");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .allowEmptyShould(true)
            .because("use cases reach the outside world only through ports (DP-05)");

    @ArchTest
    static final ArchRule portsAreSmall = classes()
            .that().resideInAPackage("..application.port..")
            .and().areInterfaces()
            .should(haveAtMostMethods(MAX_PORT_METHODS))
            .allowEmptyShould(true)
            .because("ports are small and role-specific (DP-04)");

    private static ArchCondition<JavaClass> haveAtMostMethods(int limit) {
        return new ArchCondition<>("have at most " + limit + " methods") {
            @Override
            public void check(JavaClass port, ConditionEvents events) {
                int methodCount = port.getMethods().size();
                if (methodCount > limit) {
                    String message = "%s declares %d methods".formatted(port.getName(), methodCount);
                    events.add(SimpleConditionEvent.violated(port, message));
                }
            }
        };
    }
}
