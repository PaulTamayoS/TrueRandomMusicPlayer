package com.games4science.truerandommusicplayer.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "join_playlist_track",
        foreignKeys = {
                @ForeignKey(
                        entity = Playlist.class,
                        parentColumns = "playlistId",
                        childColumns = "playlistId",
                        onDelete = ForeignKey.CASCADE // If playlist is deleted, remove links
                ),
                @ForeignKey(
                        entity = Track.class,
                        parentColumns = "uriString",
                        childColumns = "uriString",
                        onDelete = ForeignKey.CASCADE // If track is deleted, remove links
                )
        }
)
public class JoinPlaylistTrack {

    @PrimaryKey(autoGenerate = true)
    public long joinId;

    public long playlistId;

    @NonNull
    public String uriString;

    public JoinPlaylistTrack(long playlistId, @NonNull String uriString) {
        this.playlistId = playlistId;
        this.uriString = uriString;
    }
}
