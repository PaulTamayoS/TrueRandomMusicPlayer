package com.games4science.truerandommusicplayer.player;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.games4science.truerandommusicplayer.data.TrackRepository;

import java.util.List;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media.session.MediaButtonReceiver;

public class MusicService extends Service {

    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_STOP = "ACTION_STOP";

    private PlayerManager playerManager;
    private MediaSessionCompat mediaSession;
    private NotificationHelper notificationHelper;
    private AudioFocusManager audioFocusManager;

    @Override
    public void onCreate() {
        super.onCreate();

        playerManager = new PlayerManager(this);
        mediaSession = new MediaSessionCompat(this, "TrueRandomMusicPlayer");
        mediaSession.setActive(true);

        notificationHelper = new NotificationHelper(this, mediaSession);

        audioFocusManager = new AudioFocusManager(this, new AudioFocusManager.AudioFocusCallback() {
            @Override
            public void onPlay() {
                if (audioFocusManager.requestFocus()) {
                    playerManager.play();
                    updateState();
                }
            }

            @Override
            public void onPause() {
                playerManager.pause();
                updateState();
            }

            @Override
            public void onStop() {
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

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                audioFocusManager.requestFocus();
                playerManager.play();
                updateState();
            }

            @Override
            public void onPause() {
                playerManager.pause();
                updateState();
            }

            @Override
            public void onSkipToNext() {
                playerManager.next();
            }

            @Override
            public void onSkipToPrevious() {
                playerManager.previous();
            }

            @Override
            public void onStop() {
                stopSelf();
            }
        });

        List<android.net.Uri> tracks = TrackRepository.getTracks(this);
        if (tracks.isEmpty()) {
            stopSelf();
            return;
        }

        playerManager.setPlaylist(tracks);
        playerManager.play();

        startForeground(NOTIFICATION_ID,
                notificationHelper.buildNotification(true));
    }

    private void updateState() {

        boolean isPlaying = playerManager.isPlaying();

        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(
                        isPlaying ?
                                PlaybackStateCompat.STATE_PLAYING :
                                PlaybackStateCompat.STATE_PAUSED,
                        playerManager.getCurrentPosition(),
                        1f
                )
                .build();

        mediaSession.setPlaybackState(state);

        Notification notification =
                notificationHelper.buildNotification(isPlaying);

        startForeground(NOTIFICATION_ID, notification);
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        MediaButtonReceiver.handleIntent(mediaSession, intent);
//        return START_STICKY;
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {

            switch (intent.getAction()) {

                case ACTION_PLAY:
                    playerManager.play();
                    break;

                case ACTION_PAUSE:
                    playerManager.pause();
                    break;

                case ACTION_NEXT:
                    playerManager.next();
                    break;

                case ACTION_PREVIOUS:
                    playerManager.previous();
                    break;

                case ACTION_STOP:
                    stopSelf();
                    break;
            }
        }

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