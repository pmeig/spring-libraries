---
name: spring-dependency-update
description: Bump dependency versions across this Maven reactor's pom.xml files (root parent/`kotlin.version`/plugin versions, or a module-level override) - by default every non-`pmeig` dependency/property goes to its latest stable release, and any explicit `<version>` that duplicates what `spring-boot-dependencies` already manages gets removed - then migrate any code that used a method/field/class deprecated or removed by the bump, then run the affected modules' unit tests to green - adapting tests to genuinely-changed API/library behavior, or fixing production code when the bump introduced a real regression, looping until tests pass. Use when asked to update/upgrade/bump dependencies, upgrade Spring Boot/Kotlin/a library version, or "mettre à jour les dépendances/versions".
---

# spring-dependency-update

A version bump in this repo is never just an edit to `pom.xml`: this project's own history shows it routinely breaks call sites (e.g. commit `587d77b`, Spring Boot 4.0.6 -> 4.1.1, removed `AesBytesEncryptor` and forced `security/core/crypto/CryptoService.kt` onto `AesGcmBytesEncryptor.withPassword(...).build()`). Treat every bump as a four-phase loop: **bump -> compile & find breakage -> migrate -> test until green**. Don't stop after phase 1.

## 0. Scope the bump

Confirm with the user (or infer from their request) exactly which artifact(s)/version(s) to move, since this repo manages versions in three places:
- The root `pom.xml` `<parent>` block - `spring-boot-dependencies` version, which transitively pins most Spring/Spring Boot/Spring Security/Jackson versions for every module via BOM import.
- Root `pom.xml` `<properties>` - explicit `*.version` properties (`kotlin.version`, `mokito-kotlin.version`, `google-gcp.version`, `maven.test-plugin.version`, `maven.source-plugin.version`).
- A module's own `pom.xml` if it declares a dependency version directly instead of relying on the parent BOM (rare - check with `grep -n "<version>" <module>/pom.xml` first).

Do **not** touch the `pmeig-<module>.version` properties (`1.0.0-SNAPSHOT`) - those version this repo's own modules, not external dependencies.

**Default scope**: unless the user names specific artifact(s), the bump covers *every* non-`pmeig` version in the reactor - the parent, every `*.version` property, and every explicit per-dependency `<version>` in a module `pom.xml` - each moved to its latest **stable** release (skip `alpha`/`beta`/`RC`/`M`/`SNAPSHOT`/`preview` qualifiers; only match those if the user explicitly asks for a pre-release).

Discover current/available versions before editing anything:
```
mvn -q versions:display-parent-updates
mvn -q versions:display-property-updates
mvn -q versions:display-dependency-updates
```
Read the "Next available" columns and pick the highest entry that isn't a pre-release qualifier.

## 1. Bump the version(s), and drop what the BOM already manages

Edit the property/parent version directly in `pom.xml` (or the module `pom.xml` for a module-local override) with `Edit` - this is a plain text change, not a job for `versions:set` (that rewrites `<version>` of this project's own artifact, not the parent/BOM/property).

After bumping `spring-boot-dependencies`, re-check every module-level dependency that still carries an explicit `<version>`: if that artifact is also listed in the new `spring-boot-dependencies` BOM (check with `mvn -q dependency:list -pl <module> | grep <artifactId>` and compare against `mvn -q help:effective-pom`, or simply try removing the `<version>` tag and recompiling), **remove the explicit `<version>` element** and let it inherit from the parent BOM instead. Keeping a redundant explicit version defeats the point of inheriting from `spring-boot-dependencies` - it silently pins that one artifact even after future bumps. Only leave an explicit `<version>` on dependencies the BOM doesn't cover at all (e.g. `mokito-kotlin.version`, `google-gcp.version`, this repo's own `pmeig-*` artifacts).

## 2. Compile the whole reactor to surface breakage

A bump via the root POM affects every module through `dependencyManagement`, not just the one the user is thinking about. Compile everything before touching any source file:
```
mvn -q -DskipTests compile
```
Read the failures module by module. Kotlin also emits `warning: '...' is deprecated` for anything still callable but slated for removal - treat these as work items too, not just hard compile errors, since the next bump will turn them into breakage. Search explicitly if the build output truncates them:
```
mvn -q -DskipTests compile 2>&1 | grep -i deprecat
```

## 3. Migrate call sites - hook into the new official API, don't shim around it

For every compile error or deprecation warning, find what the upgraded library now recommends (release notes, `@Deprecated(message = "...")` text, or the new class/method's KDoc/Javadoc via `get_symbol_info`/`read_file` on the dependency jar) and migrate to it directly. This follows the root `CLAUDE.md` guiding principle: prefer the library's own documented replacement over a hand-rolled workaround.

Concrete template from this repo's own history (`CryptoService.kt`, Spring Security bump): the removed constructor-based `AesBytesEncryptor(secret, salt)` became the builder-based `AesGcmBytesEncryptor.withPassword(secret, salt).build()` - a like-for-like replacement type provided by the same library, not a custom encryption shim.

Re-run `mvn -q -DskipTests compile` after each fix until the reactor compiles clean with no new deprecation warnings.

## 4. Run tests per affected module, fix until green

For each module whose source changed in step 3 (and any module depending on it):
```
mvn -pl <module> -am test
```

When a test fails after the bump, decide which side is wrong before touching anything, per the user's own framing of this workflow:

- **The test is coupled to old-version specifics, not to business behavior** (a mock stub calling a now-renamed/removed method, an assertion on old exception type/message, a Mockito matcher signature that no longer resolves) and the module's actual business behavior is still correct end-to-end -> adapt the **test** to the new API, following [[unit-test-conventions]] (Mockito-kotlin imports, no mock annotations, `@Nested`-per-method, `should_..._when_...` naming). This is not "loosening" the test - the assertion on observable behavior must still hold.
- **The bump changed or broke actual behavior** (wrong output, a branch no longer reachable the way the test exercises it, a callback no longer invoked) -> fix the **production code**, not the test. Re-check the fix against the same guiding principle as step 3 (official extension point over ad hoc code) before re-running tests.

Loop steps 3-4 - re-migrate, re-test - until `mvn -pl <module> -am test` is green for every module touched by the bump. Don't consider the bump done while any module still fails to compile or test.

## 5. Final verification before reporting done

- Full reactor check: `mvn clean verify` (or `mvn -pl <module1>,<module2> -am verify` if the bump only reached a subset of modules).
- `git diff -- '*pom.xml'` should show only the intended version-related lines - no incidental formatting churn.
- Re-run the deprecation grep from step 2 one more time to confirm the bump didn't leave new warnings unaddressed.
- Follow the root `CLAUDE.md` commit message convention (type/scope derived from the current branch name) if asked to commit.
