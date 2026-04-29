package com.games4science.truerandommusicplayer.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class Playlist {

    @PrimaryKey(autoGenerate = true)
    public long playlistId;

    @NonNull
    public String playlistName;

    public Playlist(@NonNull String playlistName) {
        this.playlistName = playlistName;
    }
}
