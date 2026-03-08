package com.games4science.truerandommusicplayer.player;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
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
        player = new ExoPlayer.Builder(this).build();
        mediaSession = new MediaSession.Builder(this, player).build();
    }

    private void loadPlaylist() {
        List<MediaItem> tracks = TrackRepository.getTracks(this);

        player.clearMediaItems();
        Collections.shuffle(tracks); // TRUE RANDOM

        for (MediaItem mediaItemInList : tracks) {
            player.addMediaItem(mediaItemInList);
        }

        player.prepare();

        if (tracks.isEmpty()) {
            player.stop();
            stopSelf();
        }
        else {
            player.play();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "LOAD_PLAYLIST".equals(intent.getAction())) {
            loadPlaylist();
        }

        return super.onStartCommand(intent, flags, startId);
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
        }

        if (player != null) {
            player.release();
        }

        super.onDestroy();
    }
}