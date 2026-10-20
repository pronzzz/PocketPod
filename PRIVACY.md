# Privacy

Wheelhouse is an offline music player. In short: **nothing leaves your phone.**

- **No internet access.** The app does not request the `INTERNET` permission, so it is not able
  to connect to any server. There are no analytics, crash reporters, ads or accounts.
- **What it reads:** your music library through Android's media store (song titles, artists,
  albums, artwork). This is needed to show and play your music.
- **What it stores, on the phone only:** a cache of your library, your playlists, play counts,
  "loved" songs, your settings and the current queue.
- **Other apps:** the Extras › Apps menu lists installed apps so you can open them. This list
  is never stored or shared.

## Permissions

| Permission | Why |
| --- | --- |
| Music and audio / Storage | Read your songs and album artwork |
| Notifications (Android 13+) | Show playback controls in the notification shade and lock screen |
| Foreground service, Wake lock | Keep music playing with the screen off |
| Network state | Added by the Media3 player library to read the connection type. It cannot be used to send data. |

Uninstalling Wheelhouse removes everything it stored.
