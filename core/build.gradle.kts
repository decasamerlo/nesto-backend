plugins {
  `java-library`
}

group = "dev.nesto"
base.archivesName = "nesto-core"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(libs.lombok)

  annotationProcessor(libs.lombok)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)

  testRuntimeOnly(platform(libs.junit.bom))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
