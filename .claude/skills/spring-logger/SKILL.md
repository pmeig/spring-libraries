---
name: spring-logger
description: Explain the spring-logger module (logger/) - its purpose, correlation-id and Spring Integration log-channel machinery, dependencies, key components, test coverage, and how the quality.yml Sonar pipeline treats it. Use when asked to explain the logger module, what spring-logger does, or what it uses.
---

# spring-logger module explainer

There is no README for this module - read the files below live each time this skill runs, rather than repeating fixed prose, since the source may have changed.

## 1. Purpose and dependencies

Read `logger/pom.xml`:
- `<description>` states the module's purpose.
- `<artifactId>` is the published name (`spring-logger`).
- List dependencies, distinguishing required from `<optional>true</optional>` ones. As of writing, required deps are `spring-boot-starter-integration`, `spring-security-core`, `slf4j-api`; optional deps are `spring-boot-restclient`, `spring-boot-webclient`, and `jakarta.servlet-api` - each corresponds to a web/HTTP-client "flavor" (Servlet filter, Reactive filter, RestClient, WebClient) that the module adapts to via `@ConditionalOnClass` only when present, so the module still works with no web stack at all.

## 2. Autoconfiguration entry point

Read `logger/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` to find the registered autoconfiguration class (as of writing: `LoggerModule`, in `core/module.kt`).

## 3. Key components

Walk `logger/src/main/kotlin/pmeig/spring/libraries/logger/**` and summarize what's there. As of writing, the layout is:
- `PmeigLoggerFactory.kt` - factory returning a JDK proxy over an SLF4J `Logger` that routes log calls through Spring Integration's `logChannel` instead of logging directly.
- `beans/YamlConfiguration.kt` - SnakeYAML `Yaml` beans used for MDC property scanning.
- `core/module.kt` - `@EnablePmeigLogger` annotation and `LoggerModule` (the autoconfiguration root, `@ConditionalOnBooleanProperty("spring.plugins.pmeig.logger")`).
- `correlation/*` - `CorrelationHolder` (ThreadLocal correlation id), `CorrelationIntegration` (exposes it as a log argument/MDC entry), `CorrelationProperties`, `filter/CorrelationFilter` (servlet) and `filter/ReactiveCorrelationFilter` (reactive) that read/generate the correlation id per request, and `http/RestClientConfiguration`/`http/WebClientConfiguration` that forward it on outgoing calls.
- `integration/*` - `IntegrationBean` (wires the `logChannel` `DirectChannel` and `IntegrationFlow`), `LoggerMessage` (the message payload), `configurer/LoggerFlowConfigurer` (extension point for extra subscribers), `configurer/log/LogFlow` (default subscriber that actually calls SLF4J, with MDC and `{argument}` templating), plus argument/MDC providers (`LoggerPropertyProvider`, `LoggerUserProvider`, `SpringPropertiesMDCConfigurer`, `SpringUserMDCConfigurer`).

Re-verify this list against the actual files rather than assuming it's still accurate.

## 4. Test coverage

Check whether `logger/src/test` exists and what it covers. As of writing, there is no test directory for this module at all (the pom also has an explicit `<sonar.tests></sonar.tests>` override, consistent with that).

## 5. How .github/workflows/quality.yml treats this module

Read `.github/workflows/quality.yml`:
- Modules are auto-detected by the `context` job from changed `/src/` paths (or passed manually via `workflow_dispatch` `modules` input), then built and analyzed via `mvn clean verify -am -pl :sonar-report sonar:sonar` in the `test` job.
- The workflow passes `coverage-exclusions: '**/*module.* **/*Module.* **/WebAdvisor*'` to the `java/sonar/setup` action, which also applies its own broader default Sonar coverage exclusions (`**/configuration/**`, `**/model/**`, `**/properties/**`, `**/*Configuration.*`, `**/*Properties.*`, `**/*Helper.*`, `**/helper/**`, `**/exception/**`, `**/*Exception.*`, `**/src/test/**`).
- As of writing, `core/module.kt`'s filename matches `**/*module.*` exactly, so it's excluded from coverage - though that's moot given the module has no tests at all, so it effectively has no coverage signal in CI regardless of exclusions.

## 6. Output format

Present findings as a structured write-up with these sections: Purpose, Dependencies (required vs optional), Key components, Test coverage, How quality.yml treats it.
