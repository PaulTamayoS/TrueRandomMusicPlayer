package com.games4science.truerandommusicplayer.player;

import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.games4science.truerandommusicplayer.data.TrackRepository;

import java.util.Collections;
import java.util.List;

public class MusicService extends MediaSessionService {

    private ExoPlayer player;
    private MediaSession mediaSession;
    private boolean isPureRandomEnabled = false;
    private boolean isSearchingRandomTrack = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // This tells Android: "I am a music player, treat me like one."
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true) // 'true' enables automatic ducking!
                .setHandleAudioBecomingNoisy(true) // Pauses on headphone unplug
                .setWakeMode(C.WAKE_MODE_NETWORK) // Prepares for future API/Network streaming
                .build();

        player.setRepeatMode(Player.REPEAT_MODE_ALL);// Loop the entire shuffled list
        player.setVolume(0.30f);

        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                handleMediaItemTransition(reason);
            }
        });

        mediaSession = new MediaSession.Builder(this, player).build();
        loadPlaylist(false); // Load but don't force play immediately on boot
    }

    private void handleMediaItemTransition(int reason) {
        if (isPureRandomEnabled == true) {

            // 1. If we are already in the middle of a random jump, STOP here.
            if (isSearchingRandomTrack) {
                isSearchingRandomTrack = false; // Reset for the next time
                return;
            }

            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || // MEDIA_ITEM_TRANSITION_REASON_AUTO: means the previous song just finished
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) // MEDIA_ITEM_TRANSITION_REASON_SEEK: User clicked Next or Previous
            {
                int totalTracks = player.getMediaItemCount();
                if (totalTracks > 1) {
                    int nextRandomIndex = (int) (Math.random() * totalTracks);
                    isSearchingRandomTrack = true;
                    player.seekTo(nextRandomIndex, 0);
                }
            }
        }
    }

    private void loadPlaylist(boolean playImmediately) {
        List<MediaItem> tracks = TrackRepository.getTracks(this);

        if (tracks == null || tracks.isEmpty()) {
            Toast.makeText(this, "No Tracks into the current list !!!", Toast.LENGTH_SHORT).show(); //TODO Show it in an UI text?
            player.stop();
            player.clearMediaItems();
            return;
        }

        Collections.shuffle(tracks); // Shuffle creates a Light Random

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
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
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