import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.intellijPlatform)
    alias(libs.plugins.intellijPlatformGrammarKit)
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        phpstorm(providers.gradleProperty("platformVersion"))

        // Dieses Modul nutzt ausschliesslich Platform-APIs — keine gebuendelten Plugins noetig.

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(libs.junit4)
}

intellijPlatform {
    projectName = providers.gradleProperty("pluginNameTypoScript").get()
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
        channels = listOf("nightly")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// GrammarKit: Ausgabe landet unter build/generated/sources/... (frueher: gen/)
tasks {
    generateLexer {
        sourceFile = file("src/main/grammars/TypoScriptLexer.flex")
        pathToClass = "/com/cedricziel/idea/typoscript/lang/lexer/_TypoScriptLexer.java"
        purgeOldFiles = true
    }

    generateParser {
        sourceFile = file("src/main/grammars/TypoScriptParser.bnf")
        pathToParser = "/com/cedricziel/idea/typoscript/lang/parser/TypoScriptParser.java"
        pathToPsiRoot = "/com/cedricziel/idea/typoscript/lang/psi"
        purgeOldFiles = true
    }

    compileJava {
        dependsOn(generateLexer, generateParser)
    }
}

sourceSets {
    main {
        java.srcDirs(
            layout.buildDirectory.dir("generated/sources/grammarkit-lexer/java/main"),
            layout.buildDirectory.dir("generated/sources/grammarkit-parser/java/main"),
        )
    }
}
