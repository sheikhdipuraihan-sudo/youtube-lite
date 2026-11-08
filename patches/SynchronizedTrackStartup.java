package com.example.playerpatch;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Tracks;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reference gate for ExoPlayer 2.x. Adapt the player and track-model imports
 * to the host app; this class deliberately owns the playWhenReady transition.
 */
public final class SynchronizedTrackStartup implements Player.Listener {
    public interface SideTrackState {
        boolean requestedAudioReady(Tracks tracks);
        boolean requestedTextReady(Tracks tracks);
        boolean hasRequestedText();
        void showWaiting();
        void showFailure(@Nullable String message);
        void hideWaiting();
    }

    private final ExoPlayer player;
    private final SideTrackState sideTracks;
    private final AtomicLong generation = new AtomicLong();
    private boolean resumeAfterReady;
    private long resumePositionMs;
    private boolean waiting;

    public SynchronizedTrackStartup(ExoPlayer player, SideTrackState sideTracks) {
        this.player = player;
        this.sideTracks = sideTracks;
        player.addListener(this);
    }

    public void beginTrackChange() {
        generation.incrementAndGet();
        resumePositionMs = Math.max(0L, player.getCurrentPosition());
        resumeAfterReady = player.getPlayWhenReady();
        waiting = true;
        player.setPlayWhenReady(false);
        sideTracks.showWaiting();
    }

    public void invalidate() {
        generation.incrementAndGet();
        waiting = false;
        resumeAfterReady = false;
        sideTracks.hideWaiting();
    }

    @Override public void onTracksChanged(Tracks tracks) { tryStart(tracks); }

    @Override public void onPlaybackStateChanged(int state) {
        if (state == Player.STATE_READY) tryStart(player.getCurrentTracks());
        if (state == Player.STATE_IDLE) sideTracks.showFailure("Playback became idle");
    }

    @Override public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
        waiting = false;
        sideTracks.showFailure(error.getMessage());
    }

    private void tryStart(Tracks tracks) {
        if (!waiting || player.getPlaybackState() != Player.STATE_READY) return;
        if (!sideTracks.requestedAudioReady(tracks)) return;
        if (sideTracks.hasRequestedText() && !sideTracks.requestedTextReady(tracks)) return;
        if (player.getBufferedPosition() < resumePositionMs) return;

        waiting = false;
        player.seekTo(resumePositionMs);
        sideTracks.hideWaiting();
        player.setPlayWhenReady(resumeAfterReady);
    }
}
