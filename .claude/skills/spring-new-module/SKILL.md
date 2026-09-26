---
name: spring-new-module
description: Scaffold a new Spring Boot library module in this repo, following the same schema as the existing modules (cache/error/jpa/logger/security/swagger) - root pom.xml registration, module pom.xml, package layout, the auto-configuration entry point class, and the AutoConfiguration.imports registration file. Use when asked to create a new module, add a new Spring Boot starter/library module, or bootstrap the structure for a module in this repo.
---

# spring-new-module scaffolder

## 0. Before scaffolding: apply the repo's guiding principle

Per the root `CLAUDE.md`, check whether Spring Boot / the third-party library involved already exposes an official extension point (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, a customizer interface, etc.) before writing anything custom. Confirm with the user what the module is for and which third-party library (if any) it wraps before generating files.

## 1. Register the module in the root `pom.xml`

Read the root `pom.xml`:
- Add `<module><name-of-new-module></module>` inside the existing `<modules>` list (alongside `logger`, `cache`, `security`, `swagger`, `error`, `jpa`).
- If other modules will depend on this new one, add a `pmeig-<module>.version` property (value `1.0.0-SNAPSHOT`) next to the existing `pmeig-*.version` properties.
- Do not duplicate build/plugin config here - it's already centralized in the root POM's `<build>`/`<dependencyManagement>` and applies to every module.

## 2. Module `pom.xml`

Same shape for every module (see `swagger/pom.xml`, `jpa/pom.xml`):

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <parent>
        <groupId>io.github.pmeig</groupId>
        <artifactId>spring-libraries</artifactId>
        <version>1.0.0</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>spring-<module></artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <name>spring-<module></name>
    <description><one line stating the module's purpose></description>

    <dependencies>
        <!-- only what this module needs; mark <optional>true</optional> when the
             feature should only activate if the consuming app already has that
             dependency on its classpath - pair with @ConditionalOnClass below -->
    </dependencies>

</project>
```

## 3. Package layout

- Production code under `<module>/src/main/kotlin/pmeig/spring/libraries/<module>/...`, split into sub-packages by concern rather than dumped at the root.
- Tests mirror that structure under `<module>/src/test/kotlin/...`. Follow [[unit-test-conventions]] when writing them (Mockito imports, no mock annotations, `@Nested` per method, `should_..._when_...` naming).

## 4. Auto-configuration entry point

Use `swagger/src/main/kotlin/pmeig/spring/libraries/swagger/SwaggerAutoImport.kt` as the schema to reproduce - this is its content:

```kotlin
package pmeig.spring.libraries.swagger

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(SwaggerModule::class)
annotation class EnablePmeigSwagger

@ConditionalOnBooleanProperty(prefix = "spring.plugins.pmeig", name = ["swagger"], havingValue = true, matchIfMissing = true)
@ComponentScan(basePackageClasses = [SwaggerModule::class], excludeFilters = [ComponentScan.Filter(EnablePmeigSwagger::class)])
class SwaggerAutoImport {
}

@ComponentScan(basePackageClasses = [SwaggerModule::class], excludeFilters = [ComponentScan.Filter(EnablePmeigSwagger::class,
  SwaggerAutoImport::class)])
class SwaggerModule {
}
```

Reproduce this exact shape for the new module, substituting `<Module>` (e.g. `Notification`) and `<module>` (e.g. `notification`) for `Swagger`/`swagger`:

- `Enable Pmeig<Module>` annotation - lets a consuming app opt in **manually** via `@Import`.
- `<Module>AutoImport` class - the **automatic** entry point, guarded by `@ConditionalOnBooleanProperty(prefix = "spring.plugins.pmeig", name = ["<module>"], havingValue = true, matchIfMissing = true)` so the consuming app can disable it via a property. Add `@ConditionalOnClass` here too if the module depends on an optional third-party library.
- `<Module>Module` class - the `@ComponentScan` anchor shared by both paths, excluding the two classes above from its own scan to avoid duplicate bean registration.

**File naming**: unlike `SwaggerAutoImport.kt`, name the file after the module name without a `Spring` prefix and ending in `Module` - i.e. `<Module>Module.kt` (e.g. `SwaggerModule.kt`, `SecurityModule.kt`, `NotificationModule.kt`), placed directly in the module's root package: `<module>/src/main/kotlin/pmeig/spring/libraries/<module>/<Module>Module.kt`. The class names inside stay exactly as in the schema above (`Enable Pmeig<Module>`, `<Module>AutoImport`, `<Module>Module`) - only the physical file name changes.

## 5. Register it with Spring Boot

Create `<module>/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` containing exactly one line, the fully-qualified name of the `<Module>AutoImport` class:

```
pmeig.spring.libraries.<module>.<Module>AutoImport
```

This is Spring Boot's standard auto-configuration discovery file - do not use the legacy `spring.factories` mechanism, and do not add more than this one entry point class here.

## 6. Verify

- Confirm the new module builds: `mvn -pl <module> -am compile`.
- Confirm the module shows up in `mvn -pl <module> dependency:tree` as expected (optional deps not pulled transitively into consumers unless they opt in).
- Diff the generated `<Module>Module.kt` against the schema above one more time before reporting done.
