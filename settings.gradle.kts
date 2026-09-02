pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

plugins {
    // Laedt das benoetigte JDK (Java 25) bei Bedarf automatisch herunter
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "idea-typo3"

include("lang-fluid", "lang-typoscript", "typo3-cms")
