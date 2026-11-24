package dev.nesto;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

// Production code only, unlike ArchitectureTest: a test reaches JUnit, AssertJ and Testcontainers,
// which no adapter's list names. core's test fixtures stay in, so the in-memory fake answers to the
// persistence adapter's list too
@AnalyzeClasses(packages = "dev.nesto", importOptions = ImportOption.DoNotIncludeTests.class)
class AdapterAllowListTest {

  // A new category adds its package here and writes its own list below. A list that matches no
  // class fails, so it can't land before its category's first class
  @ArchTest
  static final ArchRule every_adapter_sits_in_a_category_that_has_an_allow_list =
      classes()
          .that()
          .resideInAPackage("dev.nesto.adapter..")
          .should()
          .resideInAnyPackage("dev.nesto.adapter.out.persistence..");

  // Grows one package at a time, in the change that first uses it. dev.nesto.domain, with no ..,
  // leaves out the domain services. Lombok is here because the members it writes carry
  // lombok.Generated
  @ArchTest
  static final ArchRule persistence_adapters_depend_only_on_their_allow_list =
      classes()
          .that()
          .resideInAPackage("dev.nesto.adapter.out.persistence..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "dev.nesto.adapter.out.persistence..",
              "dev.nesto.port.out..",
              "dev.nesto.domain",
              "java.lang..",
              "java.util..",
              "java.time..",
              "lombok..",
              "jakarta.persistence..",
              "org.hibernate.annotations..",
              "org.springframework.data.jpa.repository..",
              "org.springframework.data.repository..",
              "org.springframework.dao..",
              "org.springframework.stereotype..",
              "org.springframework.transaction.annotation..");
}
