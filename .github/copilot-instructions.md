# Repository custom instructions

This repository provides Spring Boot 4 / Kotlin library modules (`cache`, `error`, `jpa`, `logger`, `security`, `swagger`).

## Commit message convention

Derive the commit type from the current branch name prefix (case-insensitive):

- Branch starts with `feat` or `features` (e.g. `feat/xxx`, `feature/xxx`, `features/xxx`) → `feat(<main subject>): <description>`
- Branch starts with `fix`, `bugfix`, or `hotfix` (e.g. `fix/xxx`, `bugfix/xxx`, `hotfix/xxx`) → `fix(<subject>): <description>`

The `<main subject>`/`<subject>` scope is the short topic taken from the branch name (e.g. branch `feat/payment-retry` → `feat(payment-retry): add retry policy on failed webhook calls`; branch `hotfix/auth-crash` → `fix(auth-crash): guard against null session token`). The description is a concise, imperative summary of the change — not a restatement of the branch name.

Apply this convention to every commit message, and to pull request titles/descriptions, generated or suggested for this repository.
