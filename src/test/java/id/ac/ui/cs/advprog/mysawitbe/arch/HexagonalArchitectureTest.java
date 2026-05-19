package id.ac.ui.cs.advprog.mysawitbe.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "id.ac.ui.cs.advprog.mysawitbe",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..infrastructure..",
                    "org.springframework.stereotype..",
                    "org.springframework.web.."
            )
            .because("Domain layer must not depend on the project's own infrastructure layer or Spring web/stereotype annotations");

    @ArchTest
    static final ArchRule ports_must_be_interfaces = classes()
            .that().resideInAPackage("..application.port..")
            .should().beInterfaces()
            .because("Ports are contracts — they must be interfaces, not classes");

    @ArchTest
    static final ArchRule controllers_must_not_access_persistence = noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure.persistence..")
            .because("Controllers may only talk to use-case ports, not the persistence layer directly");

    @ArchTest
    static final ArchRule infrastructure_layers_must_not_cross_modules =
            slices().matching("..modules.(*).infrastructure..")
                    .should().notDependOnEachOther()
                    .because("Module infrastructure layers must not depend on each other — use application-layer events or ports");

    // Negative rule: @Service beans must not live in infrastructure.service.
    // PembayaranService and VariabelPokokService are known violators pending T1-7.
    @ArchTest
    static final ArchRule services_must_not_reside_in_infrastructure_service = noClasses()
            .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
            .and().doNotHaveSimpleName("PembayaranService")    // TODO T1-7: move to application/service
            .and().doNotHaveSimpleName("VariabelPokokService") // TODO T1-7: move to application/service
            .should().resideInAPackage("..infrastructure.service..")
            .because("@Service beans must not be placed in the infrastructure layer's service sub-package");
}
