# Contributing to Wheelhouse

Thanks for helping! Wheelhouse exists to give old phones a second life as music players, so a
few principles guide every change.

## Principles

1. **Old phones first.** It must run well on Android 5.0 with 1 GB of RAM. Prefer the platform
   over new dependencies, never block the main thread, and keep allocations out of `onDraw`.
2. **Offline and private.** No internet permission, analytics, accounts or ads.
3. **The wheel is the interface.** Every feature must be usable with the wheel alone; touch is a bonus.
4. **Calm and classic.** Fewer, better features beat many settings.

## Getting started

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for a tour of the code. Most new lists are a
few lines in `ui/screens/Menus.kt`.

## Pull requests

- Open an issue first for anything larger than a small fix, so we can agree on the approach.
- Keep pull requests focused, and include screenshots for UI changes.
- Add unit tests for pure logic (see `app/src/test`).
- `./gradlew testDebugUnitTest lintDebug` must pass, with no new `NewApi` warnings.
- Write commit messages in the imperative mood ("Add album shuffle").

## Reporting bugs

Please include your phone model, Android version, Wheelhouse version (Settings › About) and
steps to reproduce.
