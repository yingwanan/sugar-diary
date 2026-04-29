# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sugar Diary (砂糖日记) is a native Android diary application built with Kotlin and Jetpack Compose. It stores entries locally by default, supports embedded images, and offers AI-powered formatting, polishing, and emotion/psychology analysis via an OpenAI-compatible API.

## Build, Test, and Development Commands

All commands run from the project root (`sugar-diary/`).

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.localdiary.app.model.EditorDocumentParserTest"

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

Release signing requires `keystore.properties` in the project root (excluded from version control). The debug build does not require it.

The `gradle.properties` sets `android.aapt2FromMavenOverride` to a custom path (`/data/data/com.termux/files/usr/bin/aapt2`). Do not change this unless the build breaks in a different environment.

## High-Level Architecture

### Dependency Injection

The app uses manual dependency injection centered on `di/AppContainer.kt`. `DiaryApplication` creates a single `AppContainer`, and `MainActivity` passes it down to the Compose UI tree. ViewModels are instantiated directly in composables using a small `ViewModelFactory`. There is no Hilt or Koin.

### Data Layer: Dual Storage System

`DiaryRepository` is the primary repository and coordinates two distinct storage mechanisms:

- **Room Database (`AppDatabase`)**: Stores metadata—entries, emotion analyses, mood reports, style presets, psychology chat messages/analysis runs, and version snapshots. Schema version is `3` with explicit `Migration` objects defined in `AppDatabase`.
- **Local File System (`LocalEntryFileStore`)**: Stores the actual entry content (Markdown/HTML) as files on disk. `EntryEntity` in Room holds the file path, not the body text.

When adding new persistent data, decide whether it belongs in Room (structured/queryable) or on disk (large text/binary). Most new domain objects will go through `DiaryRepository` and likely into Room.

### Editor Document Model

The editor does not operate on raw Markdown strings. Instead, it uses a custom block-based document model:

- `model/EditorDocumentBlock.kt` defines blocks (text, image, format toggle).
- `model/EditorDocumentParser.kt` serializes/deserializes between the block list and the stored Markdown/HTML.
- `model/EmbeddedImageParser.kt` handles a custom image-embedding syntax used within the stored text.

Any change to how images are inserted, how formats are toggled, or how the editor serializes state must touch both the model/parser layer and the UI layer (`ui/screen/EditorScreen.kt`).

### AI and Psychology Subsystem

AI features are split across several layers:

- `data/llm/OpenAiCompatibleLlmProvider.kt`: Makes streaming HTTP calls to the configured OpenAI-compatible endpoint. `OpenAiStreamParser.kt` parses SSE chunks.
- `data/settings/AiSettingsRepository.kt`: Persists API endpoint, model names, timeout, and prompt templates via DataStore. The API key is encrypted via `security-crypto`.
- `domain/psychology/PsychologyAgentOrchestrator.kt`: Runs multi-step psychology analysis by dispatching agents from `PsychologyAgentCatalog`. Agents produce events that are saved to the database.
- `domain/report/MoodReportGenerator.kt`: Generates periodic (weekly/monthly) mood reports from emotion analysis history.

The emotion analysis pipeline sends text and image references (not raw base64) to the LLM to avoid timeouts.

### UI Architecture

- Screens live in `ui/screen/`. Each major screen has a corresponding ViewModel in `ui/viewmodel/`.
- Navigation is handled inside `DiaryAppRoot.kt` with Compose Navigation.
- `UiMessageManager` provides a simple snackbar channel for cross-screen error/success messages.

## Testing

Unit tests live in `app/src/test/java/` and use JUnit 4 plus `kotlinx-coroutines-test`. Tests cover:

- Model parsing and formatting (`model/*Test`)
- Domain logic: filtering, preview formatting, report generation, psychology agent behavior (`domain/*Test`)
- LLM stream parsing (`data/llm/OpenAiStreamParserTest`)

There are no instrumentation tests. UI testing is done manually.

## Notable Build Details

- The launcher foreground icon is generated at build time from `app/src/main/icon/ic_launcher_foreground.base64` into a PNG via a custom Gradle task (`generateLauncherIcon`). Do not edit the generated PNG directly.
- `AGENTS.md` exists in the repo root but is stale (written when the repository was empty). Treat it as non-authoritative.
- The project license is `CC BY-NC 4.0` (non-commercial).
