# SPEC

## Project
Android Torrent Stream Player

## Purpose
Develop an app that takes a magnet link, downloads the video using libtorrent, and streams it in real-time using Android Media3.

## Architecture
- **Core**: Kotlin, Jetpack Compose
- **Media**: Android Media3
- **Torrent**: libtorrent (C++ library integrated via JNI)
- **Pattern**: Hexagonal Architecture (for separation of concerns)

## Exception Handling
- Network errors (e.g., timeout, connection refused)
- Torrent download failures (e.g., invalid magnet link, network issues)
- Media playback errors (e.g., corrupted video, unsupported format)

## Completion Criteria
- [ ] Magnet link input field
- [ ] Torrent download in background
- [ ] Video streaming via Media3
- [ ] No crashes during playback
