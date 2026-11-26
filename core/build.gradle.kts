plugins {
    `java-library`
    `java-test-fixtures`
    alias(libs.plugins.spotless)
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

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter)
    testFixturesApi(libs.assertj.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        // google-java-format's own defaults, the layout its command line gives: Spotless's step
        // turns reflowLongStrings off unless told otherwise
        googleJavaFormat(libs.google.java.format.get().version).reflowLongStrings(true)
    }
}
