import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.intellijPlatform)
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        phpstorm(providers.gradleProperty("platformVersion"))

        bundledPlugins(
            "com.jetbrains.php",
            "com.intellij.css",
            "JavaScript",
            "org.jetbrains.plugins.yaml",
            "com.intellij.platform.images",
        )

        // Die beiden Sprach-Plugins sind in plugin.xml optionale Abhaengigkeiten,
        // werden zum Kompilieren und fuer runIde aber gebraucht.
        localPlugin(project(":lang-fluid"))
        localPlugin(project(":lang-typoscript"))

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(libs.junit4)
}

intellijPlatform {
    projectName = providers.gradleProperty("pluginNameCMS").get()
    buildSearchableOptions = false

    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        description = providers
            .fileContents(layout.projectDirectory.file("src/main/resources/META-INF/description.html"))
            .asText
            .map { it.replace("<html>", "").replace("</html>", "") }

        changeNotes = provider {
            with(rootProject.extensions.getByType<ChangelogPluginExtension>()) {
                renderItem(getLatest().withHeader(false).withEmptySections(false), Changelog.OutputType.HTML)
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("customSinceBuild")
            untilBuild = provider { null }
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

sourceSets {
    main {
        // typo3.dic liegt neben den Quellen und muss mit ins Jar
        resources.srcDir("src/main/java")
    }
    test {
        resources.srcDir("testData")
    }
}
