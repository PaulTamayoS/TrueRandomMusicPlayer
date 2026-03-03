package com.games4science.truerandommusicplayer.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media.session.MediaButtonReceiver;

public class NotificationHelper {

    private static final String CHANNEL_ID = "music_channel";

    private final Context context;
    private final MediaSessionCompat mediaSession;

    public NotificationHelper(Context context, MediaSessionCompat mediaSession) {
        this.context = context;
        this.mediaSession = mediaSession;
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public Notification buildNotification(boolean isPlaying) {

        int playPauseIcon = isPlaying ?
                android.R.drawable.ic_media_pause :
                android.R.drawable.ic_media_play;

        long playPauseAction = isPlaying ?
                PlaybackStateCompat.ACTION_PAUSE :
                PlaybackStateCompat.ACTION_PLAY;

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("True Random Music Player")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .addAction(
                        android.R.drawable.ic_media_previous,
                        "Previous",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                context,
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        )
                )
                .addAction(
                        playPauseIcon,
                        "PlayPause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                context,
                                playPauseAction
                        )
                )
                .addAction(
                        android.R.drawable.ic_media_next,
                        "Next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                context,
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        )
                )
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0,1,2)
                )
                .setOngoing(isPlaying)
                .build();
    }
}