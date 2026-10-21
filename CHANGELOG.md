# Changelog

All notable changes to this project are documented here.
The project follows [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-10-21

First release of **Wheelhouse**, the successor to PocketPod. The app was rebuilt from the
ground up as a home-screen launcher that turns an Android phone into a dedicated music player.

### Added
- Home launcher mode: set Wheelhouse as the default home app; the Home button always returns to the main menu.
- Touch click wheel with rotation scrolling, click sound, haptics, tap and hold actions and three speeds.
- Split-screen main menu with an animated album-art preview.
- Music browsing: Playlists, Artists, Albums, Songs, Genres and wheel-spelled Search.
- Cover Flow with perspective covers and reflections.
- Now Playing with artwork reflection, progress, volume, scrubbing, shuffle and repeat modes.
- Up Next queue, Play Next, Add to Up Next, Love, Go to Album/Artist and song info.
- Smart playlists (Loved, Recently Added, Most Played, Recently Played) and user playlists.
- Extras: Clock, Stopwatch, Sleep Timer and an Apps list for opening other apps.
- Nine device colours, light/dark screen, backlight, full-screen and keep-awake-on-charger settings.
- Lite Mode for low-RAM phones (on by default when Android reports a low-RAM device).
- Queue restore after restart, auto-return to Now Playing, background playback with media session controls.
- D-pad, keyboard and remote support.
- Automatic library refresh when music is added; "Rescan Library" also indexes files copied by hand.

### Changed
- Renamed from PocketPod to Wheelhouse; new package id `io.github.pronzzz.wheelhouse`.
- Minimum Android version lowered from 11 to 5.0 so far older phones can run it.
- Upgraded to Kotlin 2.0, Android Gradle Plugin 8.7 and Media3 1.5.

### Removed
- The unfinished PocketPod scaffold (Fragments/Navigation setup that never launched).

[1.0.0]: https://github.com/pronzzz/Wheelhouse/releases/tag/v1.0.0
