# Audio-track and subtitle selection with synchronized startup

## Findings from the supplied APK

The player is not a stock, small ExoPlayer screen. The main playback path is:

- `com.vanced.player.watch.ui.tv` — concrete watch-player controller and the object that calls `setPlayWhenReady(...)`;
- `com.vanced.player.watch.ui.main.overlay.MainVideoPlayerOverlayView` — main player controls and ExoPlayer listener bridge;
- `com.vanced.player.watch.PlayerDirector` — stream opening/preload orchestration;
- `com.vanced.extractor.host.host_interface.ytb_data.business_type.video.BusinessMediaTag` — already contains `audioTrackDisplayName`, `audioTrackId`, `audioTrackType`, and `audioTrackIsDefault`;
- `com.vanced.extractor.host.host_interface.ytb_data.business_type.analyse.BusinessCaption` — caption URL, language code, and display name;
- `com.vanced.player.watch.data.PlayerInfoDataSource` — player-info loading.

The APK already has a track-parameter callback in `MainVideoPlayerOverlayView`, but no exposed audio-track selector was found in the application player package. The selector should be added at the overlay/controller boundary, not to the unrelated ad-player classes.

## Required behavior

1. When the user selects audio or subtitles, capture `player.getCurrentPosition()` and whether the player was playing.
2. Set `playWhenReady(false)` before changing tracks.
3. Apply the audio/text selection and rebuild the media item/source. For external YouTube captions, add them as subtitle configurations before `prepare()`; do not download captions after playback has started.
4. Wait for `STATE_READY`, the requested audio/text track to appear in the loaded track groups, and a small startup buffer. A practical initial threshold is `min(750 ms, durationRemaining)` for VOD. For live streams, use the player’s live-offset/buffer policy instead of duration.
5. Seek back to the saved position, then restore `playWhenReady` only after the gate opens. If side-track loading fails, keep video paused and show a retry/fallback action rather than silently starting without the requested track.
6. Cancel the gate when the video changes or a new track request supersedes it.

## Reference helper

`patches/SynchronizedTrackStartup.java` is a compact reference implementation for an ExoPlayer 2-style player. Adapt imports and the app’s internal player interfaces to the APK’s obfuscated aliases (`o0.qh`, `o0.tm`, `o0.o5`, etc.) rather than pasting it unchanged into the decompiled tree.

The important invariant is that **one owner** controls `playWhenReady`: the track-selection callback must not call `play()` independently, and the ordinary video-ready callback must check the same gate.

## Integration points

- Add `Audio / subtitles` to the main overflow/settings action in `MainVideoPlayerOverlayView`.
- Pass the selected language/track ID to the `y` callback interface and implement it in `com.vanced.player.watch.ui.tv`.
- In `tv`, pause the player, apply the selection, and route all ready/error/track callbacks to `SynchronizedTrackStartup`.
- Extend the player-info state so the dialog receives all `BusinessMediaTag` audio-track variants and all `BusinessCaption` entries.
- Build caption `MediaItem.SubtitleConfiguration` objects before preparing the media source. Use the caption URL plus language code and `MimeTypes.TEXT_VTT` or the response MIME type.
- Keep the selected language/track ID in the existing preferences layer so the choice persists per account or globally, matching the app’s existing settings scope.

## Validation checklist

- video with two audio tracks: audio changes before playback resumes;
- subtitles enabled: no video frames are played until the first subtitle sample is available or the user explicitly chooses fallback;
- subtitles disabled: video starts normally without waiting for a text track;
- track switch during playback: position and play/pause state are preserved;
- slow caption endpoint: player remains paused, progress indicator is shown, and cancellation works;
- missing caption/audio track: clear error/fallback, no endless loading;
- next/previous video and rotation: pending gate is cancelled and cannot resume the old item.
