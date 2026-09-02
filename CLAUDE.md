# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A Gradle multi-project build producing **three separate JetBrains IDE plugins** for TYPO3 development,
published individually to the JetBrains Marketplace:

| Module | Plugin ID | Purpose |
|---|---|---|
| `typo3-cms` | `com.cedricziel.idea.typo3` | TYPO3 CMS/Extbase support on top of the PHP plugin |
| `lang-fluid` | `com.cedricziel.idea.fluid` | Fluid templating language (own lexer/parser) |
| `lang-typoscript` | `com.cedricziel.idea.typoscript` | TypoScript language (own lexer/parser) |

Each plugin is installable standalone. `typo3-cms` depends on `lang-fluid` and `lang-typoscript`
at build time but declares both as `optional` dependencies in its `plugin.xml` — features that
need them live in separate config files (`fluid-support.xml`, `typoscript-support.xml`), which
IntelliJ only loads when the other plugin is present. The same pattern applies to
`features-javascript.xml`, `features-css.xml` and, in `lang-fluid`, `php-support.xml`.
**Never put a hard reference to Fluid/TypoScript/JS classes into the main `plugin.xml`.**

## Commands

All commands run from the repository root via the Gradle wrapper (Gradle 9.7.1).
**JDK 25 is required** — IntelliJ Platform 2026.2+ mandates it. `settings.gradle.kts` applies the
foojay toolchain resolver, so Gradle provisions a matching JDK if the launcher JVM is older.

```bash
./gradlew check                       # compile + run all tests of all three modules (CI gate)
./gradlew :typo3-cms:test             # tests of a single module
./gradlew :typo3-cms:test --tests "com.cedricziel.idea.typo3.index.IconIndexTest"   # single test class
./gradlew :typo3-cms:test --tests "*IconIndexTest.testFoo"                          # single test method

./gradlew :typo3-cms:runIde           # sandbox IDE with the CMS plugin (+ fluid + typoscript)
./gradlew :lang-fluid:runIde
./gradlew :lang-typoscript:runIde

./gradlew buildPlugin                 # distributable zips into <module>/build/distributions
./gradlew verifyPluginStructure       # plugin.xml / structure validation (cheap, runs in CI's test job)
./gradlew verifyPluginProjectConfiguration  # config lint — flags e.g. a stale untilBuild
./gradlew verifyPlugin                # JetBrains Plugin Verifier; downloads a full IDE (was runPluginVerifier in 1.x)
./gradlew printBundledPlugins         # plugin IDs available in the target IDE — use to fix bundledPlugin() typos
./gradlew printProductsReleases       # the IDE builds verifyPlugin will run against
```

> **The test sandbox is not under `build/`.** IPGP 2.x keeps it in
> `.intellijPlatform/sandbox/<module>/PS-<version>/`, so `gradle clean` does **not** reset it and
> the `FileBasedIndex` under `system-test/index/` survives across runs. Tests that assert on
> `FileBasedIndex.getAllKeys()` will then see stale keys from earlier runs — `getAllKeys()`
> keeps reporting keys whose file is gone. Prefer `getContainingFiles()` to filter (see
> `TranslationIndex.findAllKeys`), and `rm -rf .intellijPlatform/sandbox` when a test result
> looks impossible.

Note: the root `test` task is configured with `scanForTestClasses false` and
`include "**/*Test.class"` — a test class **must** end in `Test` or it will silently never run.

## Target IDE version matrix

The target is a single IDE, declared in `gradle.properties` as `platformVersion` plus
`customSinceBuild`. All three modules resolve it via
`phpstorm(providers.gradleProperty("platformVersion"))` — the PHP, JavaScript, CSS, YAML and
Images plugins come bundled with PhpStorm, so there is no separate plugin version to pin any
more (the old `phpPluginVersion` / `psiViewerPluginVersion` are gone).

`untilBuild` is deliberately left open (`provider { null }`): since platform 243 JetBrains
advises against an upper bound so a plugin survives the user's next IDE update, and
`verifyPluginProjectConfiguration` warns when one is set.

To build against another version without editing the file:

```bash
ORG_GRADLE_PROJECT_platformVersion=2026.2.1 ./gradlew check
```

CI passes the same property; there is no build matrix any more. Gradle plugin versions live in
`gradle/libs.versions.toml`, not in the build scripts.

## Generated parsers (lang-fluid, lang-typoscript)

Both language modules apply `org.jetbrains.intellij.platform.grammarkit` (a subplugin of IPGP —
the standalone `org.jetbrains.grammarkit` plugin is archived). Sources live in
`src/main/grammars/*.bnf` and `*.flex`; generated Java goes to
`build/generated/sources/grammarkit-{lexer,parser}/java/main`, which each module adds to its
main source set. `compileJava` depends on `generateLexer` / `generateParser`, so a grammar
change propagates automatically — but **never edit generated code**; edit the `.bnf`/`.flex`
and rebuild. Output locations are derived from the grammar files themselves (the `package`
statement in `.flex`, `parserClass`/`psiPackage` in `.bnf`); the `pathToClass`/`pathToParser`/
`pathToPsiRoot` settings in the build scripts only drive purging and up-to-date checks and
must be kept in sync with them. Fluid parser fixtures live in
`lang-fluid/testData/com/cedricziel/idea/fluid/lang/parser` (`ParserTest`).

## Architecture of `typo3-cms`

TYPO3 configuration is plain PHP/YAML/XML with no schema, so the plugin reconstructs knowledge
about a project by **indexing** it and then exposing that through the usual IntelliJ extension
points. The recurring pipeline is:

```
FileBasedIndexExtension (index/)  ->  static query helpers on the index class
   ->  Util class (util/)  ->  ReferenceContributor / CompletionContributor / Annotator /
                               LineMarkerProvider / Inspection
```

- **`index/`** — `IconIndex`, `RouteIndex`, `TranslationIndex`, `ResourcePathIndex`,
  `TablenameFileIndex`, `ExtensionNameStubIndex`, `CoreServiceMapStubIndex`,
  `extbase/ControllerActionIndex`, `php/LegacyClassesForIDEIndex`,
  `extensionScanner/*`. Each index class carries its own `public static ID<...> KEY` and
  static accessor methods (e.g. `IconIndex.getIconDefinitionByIdentifier`) — call those rather
  than talking to `FileBasedIndex` directly. Non-string values are serialized with the
  externalizers in `index/externalizer/`; changing a stub's shape requires bumping the
  index version.
- **`psi/visitor/`** — the visitors that actually parse TYPO3 core PHP structures
  (e.g. `CoreIconParserVisitor` reading the `IconRegistry`) and feed the indexes.
- **`util/`** — stateless helpers (`ExtensionUtility`, `TranslationUtil`, `IconUtil`,
  `TCAUtil`, `ExtbaseUtility`, `PhpTypeProviderUtil`, …). Most business logic belongs here;
  the extension-point classes should stay thin.
- **`TYPO3Patterns`** — shared `PlatformPatterns`/`PhpPatterns` used by contributors to decide
  where they apply.
- **`provider/`** — PHP `typeProvider4` implementations (`GeneralUtility::makeInstance`,
  `ObjectManager::get`, `$GLOBALS[...]`, Context API). These must produce and re-resolve
  a type *signature*, see `PhpTypeProviderUtil`.
- **Feature packages** mirror TYPO3 concepts: `tca/`, `flexform/`, `routing/`, `translation/`,
  `icons/`, `site/`, `extbase/`, `userFunc/`, `dispatcher/`, `extensionScanner/`,
  `action/` (generators + `fileTemplates/` resources).

### The `pluginEnabled` gate

`TYPO3CMSProjectSettings` is a per-project `PersistentStateComponent` and **defaults to
`pluginEnabled = false`**. Any user-visible feature must early-return unless
`TYPO3CMSProjectSettings.isEnabled(project|element)` is true — inspections can extend
`codeInspection/PluginEnabledPhpInspection` / `PluginEnabledJsInspection` to get this for free.
`TYPO3CMSPostStartupActivity` offers to enable it when a TYPO3 project is detected.
Tests must therefore enable it in `setUp()`; `AbstractTestCase.prepareSettings()` already does.

## Architecture of `lang-fluid`

Beyond the generated Fluid language, this module defines **its own extension points** in
`plugin.xml` under the `com.cedricziel.idea.fluid` namespace:

- `provider.variables` (`VariableProvider`) — what `{variables}` exist at a caret
- `provider.implicitNamespace` (`NamespaceProvider`) — ViewHelper namespace bindings
- `provider.viewHelper` (`ViewHelperProvider`) — available ViewHelpers

`lang-fluid` registers the language-only implementations; PHP-aware ones
(`ControllerVariableProvider`, `PhpViewHelpersProvider`, `PhpGlobalsNamespaceProvider`) come
from `php-support.xml`, and `typo3-cms` contributes `ExtLocalconfNamespaceProvider` via
`fluid-support.xml`. To teach Fluid about a new source of variables/ViewHelpers, implement the
interface in `extensionPoints/` and register it in the right config file — do not special-case
it inside the completion contributor.

Fluid is also injected into HTML: `FluidInjector` (multiHostInjector), `FluidLanguageSubstitutor`
and `FluidHtmlExtension` make `.html` templates behave as Fluid, which is why several
extension points are registered for language `HTML`/`XML` rather than `Fluid`.

## Tests

Tests are JUnit 3-style IntelliJ platform tests (`BasePlatformTestCase`), one abstract base per
module: `AbstractTestCase` (typo3-cms) and `AbstractFluidTest` (lang-fluid). Both override
`getTestDataPath()` to `testData/com/cedricziel/idea/<module>` and provide the domain
assertions you should reuse instead of hand-rolling PSI walks —
`assertResolvesTo`, `assertHasVariant`, `assertContainsLookupElementWithText`,
`assertLineMarker`, `assertNavigationContainsFile`, `assertPhpReferenceSignatureContains`,
`assertLookupStringOnFluidCaret`, `assertContainsNamespace`, …

`typo3-cms/build.gradle` adds `src/main/java` as a resource dir (that is how `typo3.dic` ships)
and `testData/` as a test resource dir. `lang-typoscript` currently has no tests.

## Release / changelog

`CHANGELOG.md` is the single source for all three plugins, managed by the
`org.jetbrains.changelog` plugin — applied and configured **only on the root project**; the
modules read it back via `rootProject.extensions.getByType<ChangelogPluginExtension>()`. Keep an
`[Unreleased]` section with `-` items under the standard groups
(Added/Changed/Deprecated/Removed/Fixed/Security).

Each module's `intellijPlatform.pluginConfiguration` injects the latest changelog entry as change
notes and replaces the `plugin.xml` description with
`src/main/resources/META-INF/description.html`, so editing the `<description>`/`<change-notes>`
CDATA in `plugin.xml` has no effect — same for `<version>` and `<idea-version>`. The version comes
from `pluginVersion` in `gradle.properties`; CI prefixes it with the build number when publishing.

## Conventions

`.editorconfig` governs: 4 spaces (2 for `.bnf` and `.yml`), LF, UTF-8, final newline.
Code is Java (not Kotlin) throughout.
