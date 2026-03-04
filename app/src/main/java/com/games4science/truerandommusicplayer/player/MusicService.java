package com.games4science.truerandommusicplayer.player;

import android.net.Uri;

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

    @Override
    public void onCreate() {
        super.onCreate();

        // Create ExoPlayer
        player = new ExoPlayer.Builder(this).build();

        // Load saved tracks
        List<Uri> tracks = TrackRepository.getTracks(this);

        //Set Playlist
        for (Uri uri : tracks) {
            player.clearMediaItems();
            Collections.shuffle(tracks); // TRUE RANDOM
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(uri)
                    .build();
            player.addMediaItem(mediaItem);
        }

        player.prepare();

        // Create MediaSession and attach player
        mediaSession = new MediaSession.Builder(this, player)
                .build();

        // Optional: auto play when service starts
        if (!tracks.isEmpty()) {
            player.play();
        }
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

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull ControllerInfo controllerInfo) {
        return mediaSession;
    }
}