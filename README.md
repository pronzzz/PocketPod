# PocketPod
<p align="center">
  <em>"Your music. No cloud. No clutter."</em>
</p>

## Overview
PocketPod is an intentionally minimalistic, soft, nostalgic music player designed for modern touchscreens but inspired by the interaction philosophy of classic iPods. 

### Why PocketPod?
Modern music apps are bloated with social feeds, mandatory cloud sync, ads, and heavy UI frameworks. PocketPod is built to be a **local music appliance**. You put music on your phone, you open the app, and you listen.

It is specifically built and optimized for older, low-end hardware — with the primary target device being a **Samsung Galaxy J2 Pro running LineageOS Android 11**. It is designed around:
- Low RAM & Low CPU performance
- Limited storage & battery sensitivity
- Small/low-resolution displays
- Offline usage with minimal background activity

## Features
- **Appliance Mode:** A dedicated full-screen mode that hides the system UI (where possible) and shows an in-app battery indicator.
- **Local-First Architecture:** Incremental media scanning that doesn't freeze your device.
- **Intelligent Artwork Engine:** Extracts embedded artwork, uses local files (`cover.jpg`), and caches aggressively to save RAM. Optional online matching.
- **J2 Performance Mode:** A special toggle that disables expensive animations and limits memory usage to run perfectly on early Android 11 hardware.
- **No Cloud, No Telemetry:** No accounts, no data collection, no mandatory internet access.

## Architecture
See [ARCHITECTURE.md](ARCHITECTURE.md) for a detailed breakdown of the modular design (Core, Library, Artwork, Playback, UI, Performance).

## Building from Source
See [BUILD.md](BUILD.md) for instructions on compiling the project using Gradle.

## Project Phases & Walkthrough
PocketPod is being built iteratively. 
* **Phase 0:** Project Foundation (Current) - Modular structure, documentation, and empty packages.
* **Phase 1:** J2 Pro Hardware Profile - Device capability detection.
* **Phase 2:** Local Music Scanner - MediaStore integration and incremental scanning.
* *(More phases detailed in [ROADMAP.md](ROADMAP.md))*

## Privacy Philosophy
PocketPod does not collect, transmit, or monetize your data. See [PRIVACY.md](PRIVACY.md).

## License
PocketPod is open-source software. See the [LICENSE](LICENSE) file for details.
