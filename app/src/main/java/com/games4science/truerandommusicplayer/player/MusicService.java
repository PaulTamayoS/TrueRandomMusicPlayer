package com.games4science.truerandommusicplayer.player;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.media.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.games4science.truerandommusicplayer.data.TrackRepository;

import android.support.v4.media.session.MediaSessionCompat;

public class MusicService extends Service {

    private PlayerManager playerManager;
    private MediaSessionCompat mediaSession;
    private NotificationHelper notificationHelper;
    private AudioFocusManager audioFocusManager;

    @Override
    public void onCreate() {
        super.onCreate();

        playerManager = new PlayerManager(this);
        mediaSession = new MediaSessionCompat(this, "MusicService");
        notificationHelper = new NotificationHelper(this, mediaSession);
        audioFocusManager = new AudioFocusManager(this, new AudioFocusManager.AudioFocusCallback() {
            @Override
            public void onPlay() {
                if (audioFocusManager.requestFocus()) {
                    playerManager.play();
                }
            }

            @Override
            public void onPause() {
                playerManager.pause();
            }

            @Override
            public void onStop() {
                playerManager.pause();
                stopSelf();
            }

            @Override
            public void onDuck() {
                playerManager.getPlayer().setVolume(0.3f);
            }

            @Override
            public void onUnduck() {
                playerManager.getPlayer().setVolume(1.0f);
            }
        });

        playerManager.setPlaylist(TrackRepository.getTracks(this));

        startForeground(1, notificationHelper.buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        playerManager.release();
        mediaSession.release();
        audioFocusManager.abandonFocus();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
