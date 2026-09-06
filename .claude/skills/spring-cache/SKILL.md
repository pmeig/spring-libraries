---
name: spring-cache
description: Explain the spring-cache module (cache/) - its purpose, dependencies (Redis/Hazelcast backends), key components, test coverage, and how the quality.yml Sonar pipeline treats it. Use when asked to explain the cache module, what spring-cache does, or what it uses.
---

# spring-cache module explainer

There is no README for this module - read the files below live each time this skill runs, rather than repeating fixed prose, since the source may have changed.

## 1. Purpose and dependencies

Read `cache/pom.xml`:
- `<description>` states the module's purpose.
- `<artifactId>` is the published name (`spring-cache`).
- List dependencies, distinguishing required from `<optional>true</optional>` ones. Optional dependencies are pluggable backends a consuming app must add itself to opt in (as of writing: Redis via `spring-boot-starter-data-redis`, Hazelcast via `hazelcast`/`hazelcast-spring`). The required backbone is `spring-boot-starter-cache` (Spring's cache abstraction) plus `spring-boot-starter-jackson` (JSON (de)serialization of cached values).

## 2. Autoconfiguration entry point

Read `cache/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` to find the registered autoconfiguration class (as of writing: `CacheModule`, in `module.kt`, a `@ComponentScan` anchor).

## 3. Key components

Walk `cache/src/main/kotlin/pmeig/spring/libraries/cache/**` and summarize what's there. As of writing, the layout is:
- `module.kt` - `CacheModule`, the autoconfiguration/component-scan anchor.
- `core/model/CacheConfig.kt` - per-cache-name settings (ttl, name, keyPrefix, nullable, useKeyPrefix, idle).
- `core/model/CacheConfigsProperties.kt` - `@ConfigurationProperties(prefix = "spring.cache")`, exposes `configs: Map<String, CacheConfig>` for declaring per-cache config under `spring.cache.configs.<name>.*` in `application.yml`.
- `core/CacheConfigsProvider.kt` - merges `CacheConfig` beans found in the context with properties-declared ones.
- `core/CacheHandler.kt` - extension-point interface for customizing the native (Redis/Hazelcast) cache config programmatically.
- `core/JsonConfiguration.kt` - Jackson `JsonMapperBuilderCustomizer` used for serializing cached values.
- `CacheHelper.kt` - maps a generic `CacheConfig` onto backend-specific builder calls.
- `configuration/redis/*` - `RedisCacheHandler` (default handler, active unless `spring.cache.type` is set to something else) and `RedisConfiguration` (builds the `RedisCacheManagerBuilderCustomizer` bean).
- `configuration/hazelcast/*` - `HazelcastConfigProperties` (`spring.cache.hazelcast.*`: addresses, port, cluster, properties), `HazelcastCacheHandler`, `HazelcastConfiguration` (builds the Hazelcast `Config`/`HazelcastInstance`/`HazelcastCacheManager` beans, active only when Hazelcast is on the classpath).

Re-verify this list against the actual files rather than assuming it's still accurate.

## 4. Test coverage

Check whether `cache/src/test` exists and what it covers. As of writing, there is no test directory for this module at all.

## 5. How .github/workflows/quality.yml treats this module

Read `.github/workflows/quality.yml`:
- Modules are auto-detected by the `context` job from changed `/src/` paths (or passed manually via `workflow_dispatch` `modules` input), then built and analyzed via `mvn clean verify -am -pl :sonar-report sonar:sonar` in the `test` job.
- The workflow passes `coverage-exclusions: '**/*module.* **/*Module.* **/WebAdvisor*'` to the `java/sonar/setup` action, which also applies its own broader default Sonar coverage exclusions (`**/configuration/**`, `**/model/**`, `**/properties/**`, `**/*Configuration.*`, `**/*Properties.*`, `**/*Helper.*`, `**/helper/**`, `**/exception/**`, `**/*Exception.*`, `**/src/test/**`).
- Explain which of this module's own classes fall under those patterns (as of writing: `RedisConfiguration`, `HazelcastConfiguration`, `CacheConfig`, `CacheConfigsProperties`, `HazelcastConfigProperties`, `CacheHelper`, and `module.kt` all match one exclusion or another) - meaning most of the module is exempt from the Sonar coverage gate, which combined with having zero tests (see step 4) means the module currently has no real coverage signal in CI at all.

## 6. Output format

Present findings as a structured write-up with these sections: Purpose, Dependencies (required vs optional), Key components, Test coverage, How quality.yml treats it.
