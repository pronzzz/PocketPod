# PocketPod Architecture

PocketPod uses a clean, modular architecture to ensure components remain decoupled and testable.

## Packages
- `core/`: Audio utilities, database definitions, storage helpers, permission management.
- `library/`: Incremental scanner, metadata extraction, repository layer, search engine.
- `artwork/`: Embedded extraction, local file lookup, remote lookup, memory/disk cache.
- `playback/`: Media3 service, queue management, media session, UI controls.
- `ui/`: Touch-first, iPod-inspired UI screens (Home, Library, Now Playing, etc.).
- `performance/`: Device profiling and the specialized J2 Performance Mode.
