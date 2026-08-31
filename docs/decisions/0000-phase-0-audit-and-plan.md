# Phase 0: Repository Audit and Implementation Plan

## Repository Inventory
The repository is currently completely empty except for a `.git` folder and an initial, empty commit (`48ecd60`). There are no Android project files, Gradle configuration, source code, or CI scripts present.

## Current Build/Test Status
**N/A**. Since there is no project skeleton or Gradle configuration, no builds, tests, or static analysis can be run at this time.

## Existing Architecture and Dependency Summary
**N/A**. No existing code or dependencies.

## Risk Register
1. **Performance on Low-End Baseline**: The primary target is a Galaxy J2 Pro (approx. 1.5GB-2GB RAM, low-end CPU). Meeting the strict 2.5s cold start and 1s warm start budget will require extreme care with dependencies. Heavy libraries must be avoided.
2. **Original Wheel Navigation**: Designing a tactile, circular navigation model that is original (not an Apple copy) and accessible by default will be a UX challenge. It must gracefully degrade to standard touch exploration for screen readers.
3. **Storage Permission Fragmentation**: Supporting Android 11 legacy storage permissions (`READ_EXTERNAL_STORAGE`) alongside Android 13+ granular audio permissions (`READ_MEDIA_AUDIO`), and potentially SAF, while adhering to a "least privilege" philosophy, requires careful permission state management.
4. **Media Session & Background Execution**: Ensuring robust background playback and queue persistence under Android's aggressive background execution limits (especially on OEM-modified Android distributions) without foreground service violations.

## Decision: Views vs. Compose
**Decision: XML Views and Fragments.**
*Rationale:* The PRD explicitly mandates optimizing for a low-spec baseline (Galaxy J2 Pro) and advises using Views unless Compose is already present and proven efficient. Since this is a greenfield project, introducing Compose poses an unnecessary risk to our strict APK size, memory, and cold-start budgets on such constrained hardware. We will use standard AppCompat/Material Components with XML layouts.

## Proposed Data Migration Strategy
**N/A (for existing data).**
As this is a new app with no legacy user base, there are no existing SQLite databases or SharedPreferences to migrate. 
*Future strategy:* We will implement Room for local state (Library, Queue, Playlists) and use Room's standard migration paths as the schema evolves in future phases.

## Phase 1 Implementation Checklist
- [ ] Initialize the base Android Gradle project structure (Kotlin, `minSdk` 30 (Android 11), `compileSdk` 34+).
- [ ] Configure standard `.gitignore`, `gradle.properties` for optimization, and Gradle Version Catalog (`libs.versions.toml`).
- [ ] Define core theme tokens in XML (`colors.xml`, `themes.xml`, `type.xml`) favoring a dark-first, calm, and high-contrast palette.
- [ ] Create the primary `MainActivity` and setup Jetpack Navigation component (XML-based) for the single-activity shell.
- [ ] Implement placeholder `Fragment`s for the five primary destinations: Now Playing, Library, Playlists, Queue, and Settings.
- [ ] Build the empty-state onboarding UI explaining local access without forcing immediate permission requests.
- [ ] Stub a mock battery-indicator UI shell (no actual logic yet).
- [ ] Ensure all initial UI components have complete accessibility semantics (content descriptions, touch targets >= 48dp).
- [ ] Add basic UI and unit smoke tests for the navigation shell.
- [ ] Document project setup and how to run tests.
