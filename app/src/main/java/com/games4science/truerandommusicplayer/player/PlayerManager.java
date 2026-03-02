package com.games4science.truerandommusicplayer.player;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;

import java.util.List;

public class PlayerManager {

    private final ExoPlayer player;

    public PlayerManager(Context context) {
        player = new ExoPlayer.Builder(context).build();
    }

    public void setPlaylist(List<Uri> uris) {
        player.clearMediaItems();
        for (Uri uri : uris) {
            player.addMediaItem(MediaItem.fromUri(uri));
        }
        player.prepare();
    }

    public void play() { player.play(); }
    public void pause() { player.pause(); }
    public void next() { player.seekToNext(); }
    public void previous() { player.seekToPrevious(); }
    public void release() { player.release(); }

    public ExoPlayer getPlayer() { return player; }
}
