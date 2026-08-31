<div align="center">
  
# 🛞 Puffwheel

**A local-first, lightweight Android music player featuring a tactile, wheel-inspired interface.**

[![Android 11+](https://img.shields.io/badge/Android-11%2B-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](#)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-blue)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

## 📖 Overview

**Puffwheel** is a purpose-built music player for low-spec hardware (designed specifically for devices like the Galaxy J2 Pro running Android 11). It strips away the bloat of modern streaming services to deliver a lightning-fast, local-only audio experience. 

It features a unique **wheel-inspired tactile UI** designed for comfortable one-handed navigation, making it feel like a dedicated portable media player.

## ✨ Features

- 🚀 **Ultra-Fast Cold Start**: Engineered to launch in under 2.5 seconds on low-end hardware.
- 🔋 **Battery & Resource Friendly**: Uses standard Android XML Views and Fragments instead of heavy UI toolkits to preserve memory.
- 🎵 **Media3 ExoPlayer Foundation**: Robust, background-capable audio playback using the latest AndroidX Media3 APIs.
- 🛞 **Tactile Wheel UI**: A unique, circular control cluster that prioritizes ergonomics.
- 📦 **Local-First**: No internet permissions required. Your library is scanned rapidly using optimized `MediaStore` queries and cached via a robust `Room` database.
- 🖼️ **Efficient Artwork Loading**: Leverages `ContentResolver.loadThumbnail` for OOM-free album art rendering.

## 🏗️ Architecture

Puffwheel adheres to a single-activity architecture, leaning on stable and proven Android patterns to guarantee performance on constrained hardware:

- **UI Layer**: `MainActivity` hosts a `NavHostFragment` routing between `LibraryFragment` and `NowPlayingFragment`.
- **Media Layer**: `MediaPlaybackService` inherits from `MediaSessionService`, managing the lifecycle of an `ExoPlayer` instance and seamlessly handling system audio focus.
- **Data Layer**: `MediaScanner` performs off-thread synchronization with `MediaStore` and populates the `AppDatabase` (Room).
- **Optimization**: Configured with strict ProGuard/R8 rules to ensure maximum code shrinking while preserving critical reflection-based components.

## 🛠️ Development Phases

Puffwheel was systematically built in five distinct phases:

1. **App Shell & UI Foundation**: Initialized the project with Android 11 (`minSdk 30`), established a dark-first theme palette, and built the NavGraph.
2. **Local Library Access**: Integrated `Room` and Coroutines. Built a background `MediaScanner` to parse `MediaStore` audio metadata and handle storage permissions cleanly.
3. **Reliable Playback**: Integrated `androidx.media3`. Set up the foreground `MediaPlaybackService` and wired the Library list to instantly trigger playback.
4. **Polish & Now Playing UI**: Designed the wheel-inspired control layout. Implemented dynamic queue logic and memory-safe album artwork loading.
5. **Release Optimization**: Configured `proguard-rules.pro` to safeguard Room and Media3, verified R8 minification, and established final release readiness.

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana (or newer)
- JDK 17
- Android SDK 34

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/Puffwheel.git
   ```
2. Open the project in Android Studio.
3. Build and run on an emulator or physical device running Android 11 (API 30) or higher.

## 🤝 Contributing

We welcome contributions! If you're interested in improving Puffwheel, please review our [Detailed Contribution Guide](docs/CONTRIBUTING.md) to understand our architectural constraints and review processes.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
