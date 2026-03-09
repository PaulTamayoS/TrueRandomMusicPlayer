package com.games4science.truerandommusicplayer.player;

import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.MediaSession.ControllerInfo;
import com.games4science.truerandommusicplayer.data.TrackRepository;
import java.util.Collections;
import java.util.List;

public class MusicService extends MediaSessionService {

    private ExoPlayer player;
    private MediaSession mediaSession;
    private boolean isPureRandomEnabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_ALL);// Loop the entire shuffled list

        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                // REASON_AUTO_TRANSITION means the previous song just finished
                if (isPureRandomEnabled && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO_TRANSITION) {
                    int totalTracks = player.getMediaItemCount();
                    if (totalTracks > 0) {
                        int nextRandomIndex = (int) (Math.random() * totalTracks); //TODO check better formula
                        player.seekTo(nextRandomIndex, 0);
                    }
                }
            }
        });

        mediaSession = new MediaSession.Builder(this, player).build();
        loadPlaylist(false); // Load but don't force play immediately on boot
    }

    private void loadPlaylist(boolean playImmediately) {
        List<MediaItem> tracks = TrackRepository.getTracks(this);

        if (tracks.isEmpty()) {
            Toast.makeText(this, "No Tracks into the current list !!!", Toast.LENGTH_SHORT).show(); //TODO Show it in an UI text?
            player.stop();
            return;
        }

        Collections.shuffle(tracks); // TRUE RANDOM

        Toast.makeText(this, "Loading list with : " + tracks.size() + " tracks", Toast.LENGTH_SHORT).show(); //TODO Show it in an UI text?

        // Clear and update the player
        player.setMediaItems(tracks);
        player.prepare();

        if (playImmediately) {
            player.play();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("LOAD_PLAYLIST".equals(action)) {
                loadPlaylist(true);
            } else if ("TOGGLE_PURE_RANDOM".equals(action)) {
                isPureRandomEnabled = intent.getBooleanExtra("STATE", false);
                // If switching to Pure Random, we disable Repeat_ALL so our manual transition logic takes over
                player.setRepeatMode(isPureRandomEnabled ? Player.REPEAT_MODE_OFF : Player.REPEAT_MODE_ALL);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (!player.getPlayWhenReady() || player.getMediaItemCount() == 0) {
            stopSelf();
        }
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }

        super.onDestroy();
    }
}