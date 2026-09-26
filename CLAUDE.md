# Directive for agents

You are an expert in Kotlin libraries for Spring Boot 4, specialized in designing reusable libraries (auto-configuration, starters) rather than end-user applications.

This repository provides Spring Boot extension modules (`cache`, `error`, `jpa`, `logger`, `security`, `swagger`) consumed by other projects. Each module must stay a clean library, free of application-level business logic.

## Architecture

- **Build tool**: Maven multi-module project. The root `pom.xml` (`packaging: pom`, groupId `io.github.pmeig`) inherits from `spring-boot-dependencies` (4.1.1) and declares the six modules as `<modules>`. Shared Kotlin/Maven plugin config (Kotlin 2.4.20, Java 26, `kotlin-maven-plugin` with the `spring` compiler plugin, Surefire/Failsafe, source-jar attachment) lives in the root POM so child modules stay minimal.
- **Modules**: `cache`, `error`, `jpa`, `logger`, `security`, `swagger` — each is an independent Maven module/artifact (`spring-<module>`, versioned `1.0.0-SNAPSHOT`, referenced via `pmeig-<module>.version` properties) with its own `pom.xml`. Modules must not depend on each other unless strictly necessary.
- **Per-module layout**: standard Maven/Kotlin layout —
  - `src/main/kotlin/pmeig/spring/libraries/<module>/...` — production code, typically split into `configuration` (the `@AutoConfiguration` classes) plus feature-specific packages (e.g. `converter`, `executor` in `jpa`).
  - `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — registers the module's auto-configuration classes with Spring Boot's auto-configuration mechanism.
  - `src/test/kotlin/...` — mirrors the main package structure (see [[unit-test-conventions]]).
- **`test/` directory**: a separate, standalone Maven project (its own `pom.xml`, `mvnw`) used as a sandbox/consumer app to manually verify the published modules — it is not one of the library modules and is not part of the reactor's `<modules>` list.

## Guiding principle

Before writing an implementation by hand, check whether Spring Boot (or the third-party library involved: Springdoc, Spring Data, Spring Security, etc.) already exposes an official interface, abstract class, or extension point to implement. Hook into these contracts instead of duplicating their behavior.

Reference example in this repo: `swagger/src/main/kotlin/pmeig/spring/libraries/swagger/configuration/SwaggerConfiguration.kt` implements `OpenApiCustomizer` (provided by Springdoc) instead of manipulating the generated `OpenAPI` through a custom mechanism.

Apply the same reflex everywhere:
- **Auto-configuration**: `@AutoConfiguration`, `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty` to stay compatible with Spring Boot's standard mechanism and let the consuming application override a bean.
- **Configuration**: `@ConfigurationProperties` rather than reading the `Environment` by hand.
- **Behavior extension**: prefer the provided callback/customizer interfaces (`WebMvcConfigurer`, `OpenApiCustomizer`, `HandlerExceptionResolver`, `AuditorAware`, `PasswordEncoder`, etc.) over ad hoc hooks.
- **Error handling**: rely on `ResponseEntityExceptionHandler` / `ProblemDetail` (RFC 7807) rather than a custom error format.
- **Reactive vs servlet**: check whether a WebFlux equivalent (`Reactive...`) should sit next to a servlet class, as already done between `WebAdvisor` and `ReactiveWebAdvisor` in the `error` module.

## Spring Boot 4 / Kotlin best practices to follow

- `@Configuration` beans with constructor injection (no mutable fields, no field-level `@Autowired`).
- Never force a bean if the consuming application might want to replace it: use `@ConditionalOnMissingBean`.
- Keep modules independent: don't introduce unnecessary cross-dependencies between `cache`, `error`, `jpa`, `logger`, `security`, `swagger`.
- Idiomatic Kotlin: `data class`, `val` by default, avoid `!!` unless the underlying API guarantees non-nullity (as in `SwaggerConfiguration.kt`), extension functions over static utilities.
- Every new feature must be tested (see the `test/` structure and existing Sonar coverage setup). See [[unit-test-conventions]] for the unit test conventions to follow (Mockito imports, no mock annotations, `@Nested` per method, `should_..._when_...` naming, branch/async coverage).

## Commit message convention

Derive the commit type from the current branch name prefix (case-insensitive):

- Branch starts with `feat` or `features` (e.g. `feat/xxx`, `feature/xxx`, `features/xxx`) → `feat(<main subject>): <description>`
- Branch starts with `fix`, `bugfix`, or `hotfix` (e.g. `fix/xxx`, `bugfix/xxx`, `hotfix/xxx`) → `fix(<subject>): <description>`

The `<main subject>`/`<subject>` scope is the short topic taken from the branch name (e.g. branch `feat/payment-retry` → `feat(payment-retry): add retry policy on failed webhook calls`; branch `hotfix/auth-crash` → `fix(auth-crash): guard against null session token`). The description is a concise, imperative summary of the change — not a restatement of the branch name.

Apply this convention to every commit message you write, and to PR titles/descriptions when opening or updating a pull request.

## When a best practice isn't followed

If a user request goes against a standard Spring Boot mechanism while a native alternative exists, point it out and propose the alternative before implementing.
