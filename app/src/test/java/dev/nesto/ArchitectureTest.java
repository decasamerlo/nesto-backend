package dev.nesto;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

// Imports app's own tests too. core's test fixtures come in whatever the import options say,
// so leaving out app's tests alone would hold two kinds of test code to different rules
@AnalyzeClasses(packages = "dev.nesto")
class ArchitectureTest {

  @ArchTest
  static final ArchRule domain_depends_only_on_itself_the_jdk_and_lombok =
      classes()
          .that()
          .resideInAPackage("dev.nesto.domain..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("dev.nesto.domain..", "java..", "lombok..");

  @ArchTest
  static final ArchRule ports_depend_on_no_application_service =
      noClasses()
          .that()
          .resideInAPackage("dev.nesto.port..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("dev.nesto.application..");

  @ArchTest
  static final ArchRule adapters_depend_on_no_application_service =
      noClasses()
          .that()
          .resideInAPackage("dev.nesto.adapter..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("dev.nesto.application..");

  @ArchTest
  static final ArchRule adapters_depend_on_no_adapter_of_another_category =
      slices().matching("dev.nesto.adapter.(*).(*)..").should().notDependOnEachOther();

  @ArchTest
  static final ArchRule every_class_sits_in_a_package_the_rules_name =
      classes()
          .that()
          .resideInAPackage("dev.nesto..")
          .should()
          .resideInAnyPackage(
              "dev.nesto",
              "dev.nesto.domain..",
              "dev.nesto.port.in..",
              "dev.nesto.port.out..",
              "dev.nesto.application..",
              "dev.nesto.adapter.[in|out].*.*..");
}
