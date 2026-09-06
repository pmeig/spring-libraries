---
name: spring-jpa
description: Explain the spring-jpa module (jpa/) - its purpose, the generic dynamic-proxy JPA repository framework and its BigQuery implementation, dependencies, key components, test coverage, and how the quality.yml Sonar pipeline treats it. Use when asked to explain the jpa module, what spring-jpa does, or what it uses.
---

# spring-jpa module explainer

There is no README for this module - read the files below live each time this skill runs, rather than repeating fixed prose, since the source may have changed. This module is larger and more complex than the other five (`cache`, `logger`, `security`, `swagger`, `error`) - budget more file reads for it.

## 1. Purpose and dependencies

Read `jpa/pom.xml`:
- `<description>` states the module's purpose.
- `<artifactId>` is the published name (`spring-jpa`).
- List dependencies, distinguishing required from `<optional>true</optional>` ones. As of writing, required deps include `spring-boot-starter-data-jpa`, `jackson-databind`, `caffeine`, `hibernate-core`, `postgresql`, `jakarta.persistence-api` - Hibernate/Postgres are pulled in not to talk to a real database, but because `SpecificationReader` builds an in-memory Hibernate `SessionFactory` purely to translate Spring Data `Specification`/`Example`/`UpdateSpecification`/`DeleteSpecification` criteria into native SQL text. Optional deps are `spring-security-core` (default `AuditorAware`/`ReactiveAuditorAware` beans), `spring-cloud-gcp-starter-bigquery` (the entire BigQuery integration, gated by `@ConditionalOnClass(BigQuery::class)`), and `spring-context-support` (Caffeine cache manager wiring).

## 2. Autoconfiguration entry point

Read `jpa/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` to find the registered autoconfiguration class (as of writing: `JpaAutoImport`, a `@ComponentScan` over two sub-packages: `CoreModule` and `BigQueryModule`).

## 3. Key components

Walk `jpa/src/main/kotlin/pmeig/spring/libraries/jpa/**`. As of writing the module splits into two halves - re-verify this split and the class names against the actual source, since this is the most actively-developed module:

**Generic core (`core/**`)** - reusable JPA/repository plumbing, not BigQuery-specific:
- `FieldAccessor.kt` - reflection-based get/set abstractions (with nested/embedded field chain support via `ParentFieldAccessor`).
- `column/**` - reusable `@MappedSuperclass` building blocks combining id strategy (auto/UUID) x audit columns x enabled flag.
- `auditing/AuditingManager.kt` - applies `@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`/`@LastModifiedBy` via cached field accessors.
- `converter/specification/SpecificationReader.kt` - the in-memory-Hibernate-to-SQL-text translator mentioned above.
- `entity/EntityAnnotationReader.kt` - reads `@Entity`/`@Table`/`@Id`/`@Column`/`@Struct` via reflection into cached `DataMetadata`.
- `executor/**` + `registrar/DataJpaRepositorySupportRegistrar.kt` - the generic dynamic-proxy repository framework: scans for annotated repository interfaces, builds a chain of `JpaMethodInvoker`s, and registers a JDK/cglib proxy implementing them.

**BigQuery implementation (`data/bigquery/**`)** - the concrete backend:
- `annotation/BigQueryRepository.kt` - the `@BigQueryRepository` marker annotation.
- `client/BigQueryClient.kt` (+ `BigQueryClientSimple`, `BigQueryMultiClient`, `BigQueryMultiBatchClient`) - table CRUD and query execution against BigQuery.
- `mapper/**` + `mapper/factory/BigQueryMapperFactory.kt` - resolves and caches `BigQueryMapper<T>` implementations converting between Kotlin/Java types and BigQuery `FieldValue`/`QueryParameterValue` (dates, primitives, structs/arrays).
- `registrar/BigQueryRepositoryRegistrar.kt` + `repository/SimpleJpaBigQueryRepository.kt` (standard CRUD/Specification), `repository/query/QueryJpaBigQueryRepository.kt` (`@Query`-annotated methods), `repository/MethodNameBigQueryJpaRepository.kt` (Spring-Data-style derived query methods) - the three `JpaMethodInvoker` chain links behind `@BigQueryRepository` interfaces.

Re-verify this list against the actual files rather than assuming it's still accurate.

## 4. Test coverage

Check whether `jpa/src/test` exists and what it covers. As of writing, it exists with roughly 20 Kotlin files giving moderate real coverage: entity/metadata reading (`EntityAnnotationReaderTest`), per-type-family mapper tests (dates/primitives/structs/pagination SQL), BigQuery client tests, and repository registrar/dispatch tests (`SimpleJpaBigQueryRepositoryTest`, `QueryJpaBigQueryRepositoryTest`, `BigQueryRepositoryRegistrarInjectionTest`) - this is the best-tested of the six modules in the repo.

## 5. How .github/workflows/quality.yml treats this module

Read `.github/workflows/quality.yml`:
- Modules are auto-detected by the `context` job from changed `/src/` paths (or passed manually via `workflow_dispatch` `modules` input), then built and analyzed via `mvn clean verify -am -pl :sonar-report sonar:sonar` in the `test` job.
- The workflow passes `coverage-exclusions: '**/*module.* **/*Module.* **/WebAdvisor*'` to the `java/sonar/setup` action, which also applies its own broader default Sonar coverage exclusions (`**/configuration/**`, `**/model/**`, `**/properties/**`, `**/*Configuration.*`, `**/*Properties.*`, `**/*Helper.*`, `**/helper/**`, `**/exception/**`, `**/*Exception.*`, `**/src/test/**`).
- As of writing, only `core/CoreModule.kt` and `data/module/BigQueryModule.kt` match `**/*Module.*` (both trivial `@ComponentScan` markers, low-risk to exclude). Everything else - mappers, registrars, clients, converters - is in-scope for coverage, and unlike the other five modules, this one actually has real tests behind most of it (step 4).

## 6. Output format

Present findings as a structured write-up with these sections: Purpose, Dependencies (required vs optional), Key components, Test coverage, How quality.yml treats it.
