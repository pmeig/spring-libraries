---
name: unit-test-conventions
description: Conventions to follow when writing or reviewing Kotlin unit tests (TU / tests unitaires) with Mockito in this repo - import style, no annotations for mocks, @Nested per method, should_..._when_... naming, and maximizing branch/async coverage. Use whenever writing, editing, or reviewing a *Test.kt file, or when the user says "TU", "tests unitaires", or asks to write/fix tests.
---

# Unit test conventions (Kotlin + Mockito)

## 1. Mockito imports

Always import Mockito functions individually (the Kotlin equivalent of Java's `import static`), never through a qualified `Mockito.xxx(...)` call in the test body.

Prefer `org.mockito.kotlin.*` (already a dependency of this repo) since it exposes idiomatic top-level functions and avoids Kotlin reserved-word issues (`when`):

```kotlin
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.argumentCaptor
```

If `org.mockito.kotlin` has no equivalent for a specific need, import the Java static method directly (`import org.mockito.Mockito.mock`) rather than calling `Mockito.mock(...)` inline.

## 2. No annotations for mocks

Do not use `@Mock`, `@InjectMocks`, or `@ExtendWith(MockitoExtension::class)` unless the test genuinely requires it (e.g. `@Captor` with complex injection). Create mocks explicitly and build the SUT by hand:

```kotlin
class MyServiceTest {

  private val dependency: Dependency = mock()

  private val service = MyService(dependency)
  ...
}
```

This keeps the test readable without framework magic and makes what's mocked explicit.

## 3. Structure: one `@Nested` class per tested method

Each public method of the SUT gets its own `@Nested` inner class, named after the method (PascalCase):

```kotlin
@Nested
inner class Encrypt {
  @Test fun should_...() { ... }
}

@Nested
inner class Decrypt {
  @Test fun should_...() { ... }
}
```

Existing reference in the repo: `security/src/test/kotlin/pmeig/spring/libraries/security/core/crypto/CryptoServiceTest.kt`.

## 4. `should_..._when_...` naming convention

Every test follows the `should_<expected result>_when_<condition>` format in snake_case for the function name:

```kotlin
@Test
fun should_return_empty_string_when_value_is_null() { ... }

@Test
fun should_throw_exception_when_input_is_invalid() { ... }

@Test
fun should_call_success_callback_when_operation_completes() { ... }
```

Do not use free-form sentence test names in backticks (`` `should return X` ``): the `should_..._when_...` format must stay grep-able and consistent everywhere.

## 5. Goal: maximum branch and async coverage

The goal of every test suite is to make execution go through **all** branches and **all** deferred behaviors of the tested code, not just the happy path:

- For every `if`/`else`, `when`/`else`, `try`/`catch`, elvis operator `?:`, or safe call `?.`: write at least one test per branch.
- For every functional parameter (lambda, `Runnable`, `Supplier`, callback, higher-order function): the test must force it to actually execute, not just verify it was passed as an argument. Use `doAnswer { it.getArgument<...>(0).invoke() }` or the equivalent to trigger the callback captured by the mock.
- For any asynchronous code (coroutines, `CompletableFuture`, reactive `Mono`/`Flux`, `@Async`): explicitly await/consume the result (`runTest`/`runBlocking`, `.get()`/`.join()`, `StepVerifier`) rather than letting the test finish before the deferred processing actually runs.
- Verify side effects with `verify(mock).method(...)` rather than relying only on return-value assertions, when path coverage depends on a call actually being triggered.

Before considering a method covered, re-read its code and mentally list every possible branch/execution path, then check that a test exists for each one.
