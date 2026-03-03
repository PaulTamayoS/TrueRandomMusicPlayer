package com.games4science.truerandommusicplayer.player;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.audio.AudioAttributes;


import java.util.Collections;
import java.util.List;

public class PlayerManager {

    private final ExoPlayer player;

    public PlayerManager(Context context) {
        player = new ExoPlayer.Builder(context).build();

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player.setAudioAttributes(attributes, false);
    }

    public void setPlaylist(List<Uri> uris) {
        player.clearMediaItems();
        Collections.shuffle(uris); // TRUE RANDOM
        for (Uri uri : uris) {
            player.addMediaItem(MediaItem.fromUri(uri));
        }
        player.prepare();
        player.setShuffleModeEnabled(true);
    }

    public void play() { player.play(); }
    public void pause() { player.pause(); }
    public void next() { player.seekToNext(); }
    public void previous() { player.seekToPrevious(); }
    public boolean isPlaying() { return player.isPlaying(); }
    public long getCurrentPosition() { return player.getCurrentPosition(); }
    public void release() { player.release(); }
    public ExoPlayer getPlayer() { return player; }
}