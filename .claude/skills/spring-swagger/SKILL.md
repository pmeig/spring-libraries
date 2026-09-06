---
name: spring-swagger
description: Explain the spring-swagger module (swagger/) - its purpose, OpenAPI/springdoc setup, security lock-down, dependencies, key components, test coverage, and how the quality.yml Sonar pipeline treats it. Use when asked to explain the swagger module, what spring-swagger does, or what it uses.
---

# spring-swagger module explainer

There is no README for this module - read the files below live each time this skill runs, rather than repeating fixed prose, since the source may have changed.

## 1. Purpose and dependencies

Read `swagger/pom.xml`:
- `<description>` states the module's purpose.
- `<artifactId>` is the published name (`spring-swagger`).
- List dependencies, distinguishing required from `<optional>true</optional>` ones. As of writing, both `springdoc-openapi-starter-webmvc-ui` and `springdoc-openapi-starter-webflux-ui` are required (the module only activates one side at runtime via `@ConditionalOnWebApplication`, but both starters are always pulled in); optional deps are `jakarta.servlet-api`, `spring-security-web`, `spring-web`, `spring-security-config` - the Swagger-UI security lock-down only activates when Spring Security is actually on the classpath.

## 2. Autoconfiguration entry point

Read `swagger/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` to find the registered autoconfiguration class (as of writing: `SwaggerAutoImport`, which also defines the `@EnablePmeigSwagger` annotation and the `SwaggerModule` component-scan anchor).

## 3. Key components

Walk `swagger/src/main/kotlin/pmeig/spring/libraries/swagger/**` and summarize what's there. As of writing, the layout is:
- `SwaggerAutoImport.kt` - `@EnablePmeigSwagger` annotation, the `SwaggerAutoImport` autoconfig class, and `SwaggerModule` (the `@ComponentScan` anchor).
- `annotation/Rest.kt` - `@RestMapping` meta-annotation merging Spring's `@RequestMapping` with Swagger's `@Operation`, plus verb-specific shorthand annotations `@Get`/`@Post`/`@Put`/`@Delete`/`@Patch`/`@Head`/`@Options`/`@Trace`.
- `configuration/SwaggerConfiguration.kt` - `OpenApiCustomizer` that strips `-controller`/`-endpoint` suffixes from generated OpenAPI tag names.
- `configuration/security/ServletSwaggerSecurity.kt` and `ReactiveSwaggerSecurity.kt` - permit-all Swagger UI/webjars/API-docs paths, highest precedence, active only when Spring Security is on the classpath (Servlet vs Reactive respectively).

Re-verify this list against the actual files rather than assuming it's still accurate.

## 4. Test coverage

Check whether `swagger/src/test` exists and what it covers. As of writing, a `src/test/resources` directory exists but contains no files at all - effectively no real test coverage.

## 5. How .github/workflows/quality.yml treats this module

Read `.github/workflows/quality.yml`:
- Modules are auto-detected by the `context` job from changed `/src/` paths (or passed manually via `workflow_dispatch` `modules` input), then built and analyzed via `mvn clean verify -am -pl :sonar-report sonar:sonar` in the `test` job.
- The workflow passes `coverage-exclusions: '**/*module.* **/*Module.* **/WebAdvisor*'` to the `java/sonar/setup` action, which also applies its own broader default Sonar coverage exclusions (`**/configuration/**`, `**/model/**`, `**/properties/**`, `**/*Configuration.*`, `**/*Properties.*`, `**/*Helper.*`, `**/helper/**`, `**/exception/**`, `**/*Exception.*`, `**/src/test/**`).
- Worth flagging: as of writing, the `SwaggerModule` class lives inside `SwaggerAutoImport.kt`, so the **class name** matches `**/*Module.*` but the **file name** doesn't - the exclusion glob matches file paths, not class names, so `SwaggerModule` is *not* actually exempted despite the naming convention suggesting it should be. `SwaggerConfiguration`, `ReactiveSwaggerSecurity`/`ServletSwaggerSecurity`, however, do match `**/*Configuration.*` via the sonar-setup action's default exclusions. Combined with having no real tests, this module has little to no coverage signal in CI.

## 6. Output format

Present findings as a structured write-up with these sections: Purpose, Dependencies (required vs optional), Key components, Test coverage, How quality.yml treats it.
