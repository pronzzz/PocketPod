# Performance Targets

PocketPod must run smoothly on a Samsung Galaxy J2 Pro (Android 11).

- **Startup:** < 3 seconds cold launch.
- **Memory:** Aggressive LRU caching for artwork.
- **Scanning:** Incremental only; never block the main thread.
- **Battery:** Minimal wakelocks and polling.
