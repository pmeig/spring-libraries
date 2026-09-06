---
name: spring-security
description: Explain the spring-security module (security/) - its purpose, JWT/cache authentication strategies, annotation-driven authorization, dependencies, key components, test coverage, and how the quality.yml Sonar pipeline treats it. Use when asked to explain the security module, what spring-security does, or what it uses.
---

# spring-security module explainer

There is no README for this module - read the files below live each time this skill runs, rather than repeating fixed prose, since the source may have changed.

## 1. Purpose and dependencies

Read `security/pom.xml`:
- `<description>` states the module's purpose.
- `<artifactId>` is the published name (`spring-security`).
- List dependencies, distinguishing required from `<optional>true</optional>` ones. As of writing, required deps include `jjwt-api`, `spring-logger` (this repo's own logger module), `kotlinx-coroutines-reactor`, `jackson-module-kotlin`, and `spring-security-core`/`-web`/`-config`; optional deps are `jakarta.servlet-api` and `spring-webflux` (Servlet vs Reactive support) and `spring-cache` (this repo's own cache module - only needed for the cache-based authentication strategy, activated via `@ConditionalOnClass(CacheConfig::class)`).

## 2. Autoconfiguration entry point

Read `security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` to find the registered autoconfiguration class (as of writing: `PmeigSecurityAutoConfiguration`, in `core/Security.kt`). Note: as of writing that class's `@AutoConfiguration` annotation is commented out in the source - it still works as an autoconfig class purely because it's listed in the `.imports` file. Flag this if still true, since it's a bit of dead/vestigial code worth knowing about.

## 3. Key components

Walk `security/src/main/kotlin/pmeig/spring/libraries/security/**` and summarize what's there. As of writing, the layout covers two authentication strategies plus a shared authorization/token framework:
- `authentication/jwt/*` - `JwtService` (create/parse tokens via JJWT), `JwtAuthenticationProvider`, `JwtProperties` (`spring.security.pmeig.auth.jwt.*`), `JwtAlgorithm` (supported signature algorithms), login controllers (`JwtLoginController` servlet / `ReactiveJwtLoginController` reactive), and claim/config handlers (`JwtClaimHandler`, `JwtConfigurer`).
- `authentication/cache/*` - `SecurityCache` (looks up cached `Authentication` by correlation id), `SecurityCacheConfiguration`, `UserCacheProperties` - only active when the optional `spring-cache` dependency is present.
- `authentication/core/modules.kt` - per-strategy DI modules: `JwtModule`, `JwtCoreModule`, `CacheModule` (`@ConditionalOnProperty` on `spring.security.pmeig.auth.type`).
- `core/annotation/*` - `@PmeigSecurity` meta-annotation and derived `@Public`/`@Denied`/`@Roles`/`@Features`/`@NoRoles`/`@NoFeatures`, plus `SecurityAnnotationService` which reflectively scans `@RestController`s to build authorization rules.
- `core/authorization/*` - `SecurityAuthorization` (resolved rule model), `SecurityAuthorizationConfiguration` (applies rules to `HttpSecurity`/`ServerHttpSecurity`), `SecurityAuthorizationProperties` (YAML-declared rules).
- `core/filter/*` - `ServletSecurityFilter`/`ReactiveSecurityFilter` chaining all `SecurityAuthenticationProvider`s.
- `core/manager/*` - `TokenCookieManager`/`TokenHeaderManager`/`TokenBodyManager` (where the token is carried).
- `core/crypto/CryptoService` - AES encrypt/decrypt used to protect JWT-embedded passwords.
- `core/model/SecurityAdapter` - abstracts Servlet vs Reactive authorization DSL differences.

Re-verify this list against the actual files rather than assuming it's still accurate.

## 4. Test coverage

Check whether `security/src/test` exists and what it covers. As of writing, it exists but only contains `core/crypto/CryptoServiceTest.kt` - the rest of this fairly large module (JWT flow, authorization annotations, filters, token managers) is untested.

## 5. How .github/workflows/quality.yml treats this module

Read `.github/workflows/quality.yml`:
- Modules are auto-detected by the `context` job from changed `/src/` paths (or passed manually via `workflow_dispatch` `modules` input), then built and analyzed via `mvn clean verify -am -pl :sonar-report sonar:sonar` in the `test` job.
- The workflow passes `coverage-exclusions: '**/*module.* **/*Module.* **/WebAdvisor*'` to the `java/sonar/setup` action, which also applies its own broader default Sonar coverage exclusions (`**/configuration/**`, `**/model/**`, `**/properties/**`, `**/*Configuration.*`, `**/*Properties.*`, `**/*Helper.*`, `**/helper/**`, `**/exception/**`, `**/*Exception.*`, `**/src/test/**`).
- Worth flagging: as of writing, `authentication/core/modules.kt` and `core/Security.kt` define classes named `JwtModule`/`CacheModule`/`SecurityModule`, but their **filenames** (`modules.kt` is plural, `Security.kt` doesn't contain "module" at all) don't match the `**/*module.* **/*Module.*` glob the way `logger`'s exact `module.kt` does - so this DI-wiring code is *not* exempted and counts toward/against the coverage gate despite being comparably low-value. Combined with the near-total lack of tests (step 4), this module likely fails or barely scrapes the coverage gate.

## 6. Output format

Present findings as a structured write-up with these sections: Purpose, Dependencies (required vs optional), Key components, Test coverage, How quality.yml treats it.
