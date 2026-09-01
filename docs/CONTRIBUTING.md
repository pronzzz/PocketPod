# Contributing to PocketPod

Thank you for your interest in contributing to PocketPod! To ensure the project remains true to its core vision, please read this guide before submitting pull requests.

## 🎯 Project Philosophy

PocketPod is built under strict constraints to serve low-end hardware. Our primary target is the **Galaxy J2 Pro running LineageOS (Android 11)**. 

### Core Tenets:
1. **Performance First**: The app MUST launch in under 2.5 seconds.
2. **Minimal Memory Footprint**: We actively avoid heavy UI toolkits. This is why PocketPod uses **XML Views and Fragments** instead of Jetpack Compose.
3. **Local-Only**: No network requests, no analytics, no cloud sync. The app must remain entirely functional offline and respect user privacy.
4. **Ergonomic Design**: The UI should feel like a dedicated media player, centered around the tactile, wheel-inspired interface.

## 🛠️ Development Guidelines

### 1. Dependency Management
Before adding a new dependency to `gradle/libs.versions.toml`, ask yourself: *Is this absolutely necessary?* 
- Avoid large libraries if the Android Framework provides a sufficient alternative.
- Always check the impact on APK size and cold-start time.

### 2. UI Architecture
- Stick to **XML Views**. Do not introduce Jetpack Compose.
- Use `ConstraintLayout` to keep view hierarchies flat.
- Handle image loading intelligently. Use `ContentResolver.loadThumbnail` for artwork to prevent `OutOfMemoryError` on 1.5GB RAM devices.

### 3. Media Playback
- All playback logic must interface through `androidx.media3`.
- Do not implement custom background services for playback; always rely on `MediaSessionService`.

### 4. Code Style
- Write idiomatic Kotlin.
- Use Coroutines (`Dispatchers.IO`) for database and `MediaStore` operations. NEVER block the main thread.
- Ensure all new classes and reflection-based components (like Room entities) are documented in `app/proguard-rules.pro` to survive R8 minification safely.

## 🔄 Pull Request Process

1. **Open an Issue First**: Before writing code, open an issue to discuss your proposed changes. This ensures your work aligns with the project's strict hardware constraints.
2. **Branch Naming**: Use descriptive branch names (e.g., `feature/queue-reordering` or `bugfix/artwork-oom`).
3. **Commit Messages**: Write clear, imperative commit messages.
4. **Testing**:
   - Run `./gradlew lintDebug` to catch potential memory leaks or unused resources.
   - Run `./gradlew assembleRelease` to ensure your code survives R8 minification.
5. **Review**: Wait for a maintainer to review. We will specifically look at the performance implications of your PR.

We look forward to building a beautifully efficient music player together!
