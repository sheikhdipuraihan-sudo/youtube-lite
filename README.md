# YouTube Lite player patch workspace

This repository was empty at the referenced commit (`8da9f98`, `Empty repository`). The supplied APK was therefore decompiled separately under `/home/ubuntu/decompiled_YouTubeLite`; it was not possible to produce a genuine modified APK from this repository without importing the original Android source or a complete buildable project.

The intended feature is documented in [`docs/player-track-change.md`](docs/player-track-change.md). It adds:

- an audio-track selector;
- a subtitle-language selector;
- pre-play buffering for the selected audio/subtitle tracks;
- a single gate that starts playback only after the video and requested side tracks are ready;
- preservation of the current position when a track changes.

The GitHub Actions workflow in `.github/workflows/build-apk.yml` builds a normal Gradle Android project once source is added. It intentionally fails early with a clear message while this repository remains empty.
