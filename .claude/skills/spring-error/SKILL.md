---
name: spring-error
description: Explain the spring-error module (error/) - its purpose, business/technical exception hierarchy, generic advisor, dependencies, key components, test coverage, and how the quality.yml Sonar pipeline treats it. Use when asked to explain the error module, what spring-error does, or what it uses.
---

# spring-error module explainer

There is no README for this module - read the files below live each time this skill runs, rather than repeating fixed prose, since the source may have changed.

## 1. Purpose and dependencies

Read `error/pom.xml`:
- `<description>` states the module's purpose.
- `<artifactId>` is the published name (`spring-error`).
- List dependencies. As of writing there are no `<optional>true</optional>` dependencies in this module: `spring-webmvc` is scope `provided`, and `spring-logger` (this repo's own logger module) is a required internal dependency used for logging via `PmeigLoggerFactory`.

## 2. Autoconfiguration entry point

Read `error/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` to find the registered autoconfiguration class (as of writing: `ErrorModule`, a plain `@ComponentScan` over the module's own package).

## 3. Key components

Walk `error/src/main/kotlin/pmeig/spring/libraries/error/**` and summarize what's there. As of writing, the layout is:
- `ErrorModule.kt` - the autoconfiguration entry point.
- `core/exception/TechnicalException.kt` / `BusinessException.kt` - checked exception hierarchy (`BusinessException` extends `TechnicalException`, adds a `business` payload).
- `core/runtime/RuntimeTechnicalException.kt` / `RuntimeBusinessException.kt` - unchecked mirrors of the two checked exceptions.
- `web/WebException.kt` - a `RuntimeTechnicalException` subtype adding HTTP-facing fields (`status`, `body`, `headers`), an `EXCEPTION_CODE_HEADER` response header, and `toResponse()`/`toReactiveResponse()` builders for servlet/reactive responses.
- `web/advisor/WebAdvisorAdapter.kt` - abstract base with a generic `@ExceptionHandler(WebException::class)` that logs the error and delegates to an injected mapper to produce the concrete response type.
- `web/advisor/WebAdvisor.kt` - concrete `@ControllerAdvice` classes (`ServletAdvisor`, `ReactiveAdvisor`) extending `WebAdvisorAdapter`, active based on `@ConditionalOnWebApplication` type.

Re-verify this list against the actual files rather than assuming it's still accurate.

## 4. Test coverage

Check whether `error/src/test` exists and what it covers. As of writing, there is no test directory for this module at all.

## 5. How .github/workflows/quality.yml treats this module

Read `.github/workflows/quality.yml`:
- Modules are auto-detected by the `context` job from changed `/src/` paths (or passed manually via `workflow_dispatch` `modules` input), then built and analyzed via `mvn clean verify -am -pl :sonar-report sonar:sonar` in the `test` job.
- The workflow passes `coverage-exclusions: '**/*module.* **/*Module.* **/WebAdvisor*'` to the `java/sonar/setup` action, which also applies its own broader default Sonar coverage exclusions (`**/configuration/**`, `**/model/**`, `**/properties/**`, `**/*Configuration.*`, `**/*Properties.*`, `**/*Helper.*`, `**/helper/**`, `**/exception/**`, `**/*Exception.*`, `**/src/test/**`).
- As of writing, `ErrorModule.kt` matches `**/*Module.*` and `web/advisor/WebAdvisor.kt` matches `**/WebAdvisor*` explicitly - this is the module the `WebAdvisor*` exclusion pattern was clearly written for. The default `**/*Exception.*`/`**/exception/**` exclusions also cover the entire exception hierarchy. That leaves essentially only `WebException` and `WebAdvisorAdapter` as candidates for coverage - and since there are no tests at all, this module has zero real coverage signal in CI, almost entirely masked by exclusions.

## 6. Output format

Present findings as a structured write-up with these sections: Purpose, Dependencies (required vs optional), Key components, Test coverage, How quality.yml treats it.
