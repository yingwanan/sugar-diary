# Repository Guidelines

## Project Structure & Module Organization
This repository is currently empty, so keep the initial layout simple and predictable as code is added.

- Put application code in `src/`.
- Put tests in `tests/`, mirroring the `src/` structure.
- Put static files in `assets/`.
- Put developer tooling or automation in `scripts/`.
- Keep root-level files limited to project metadata, CI, and contributor docs.

Example:
`src/diary/entry.py` should have its test in `tests/diary/test_entry.py`.

## Build, Test, and Development Commands
No build system or package manifest is committed yet. Add a small, explicit command surface when tooling is introduced, and document it in this file and the main README in the same change.

Preferred command names:

- `make dev`: start the local development workflow.
- `make test`: run the full test suite.
- `make lint`: run formatting and static checks.
- `make build`: create a production artifact, if applicable.

If you use another toolchain, expose equivalent commands such as `npm test` or `pytest`.

## Coding Style & Naming Conventions
Follow the defaults of the language you introduce, but keep naming consistent across the repo.

- Use `kebab-case` for Markdown files, scripts, and asset names.
- Use `snake_case` for Python modules and test files.
- Use `PascalCase` only for type or component names when the language expects it.
- Keep modules focused; prefer one clear responsibility per file.
- Use formatters and linters once configured; do not mix competing tools.

## Testing Guidelines
There is no test framework configured yet. New features should include automated tests alongside the first implementation.

- Name tests after the unit under test, for example `test_entry.py` or `entry.test.ts`.
- Cover core logic and failure paths before UI or integration polish.
- Keep fixtures small and local to the test module unless shared broadly.

## Commit & Pull Request Guidelines
Git history is not available in this workspace, so use a clear baseline convention.

- Write commit subjects in the imperative mood, under 72 characters.
- Prefer prefixes such as `feat:`, `fix:`, `docs:`, `test:`, and `chore:`.
- Keep each pull request scoped to one change set.
- Include a short description, testing notes, and screenshots for UI changes.
- Update docs whenever you add new commands, structure, or setup steps.
