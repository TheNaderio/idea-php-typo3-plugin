import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.changelog)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Eine zentrale CHANGELOG.md fuer alle drei Plugins.
// Die Module lesen daraus ueber rootProject.extensions ihre changeNotes.
changelog {
    version = providers.gradleProperty("pluginVersion")
    path = file("CHANGELOG.md").canonicalPath
    headerParserRegex = """\d+\.\d+\.\d+""".toRegex()
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
}

subprojects {
    apply(plugin = "java")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(
                providers.gradleProperty("javaVersion").get().toInt()
            )
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        // Plattform-Tests im JUnit3-Stil: Klassennamen MUESSEN auf *Test enden,
        // sonst werden sie stillschweigend nicht ausgefuehrt.
        isScanForTestClasses = false
        include("**/*Test.class")
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}
