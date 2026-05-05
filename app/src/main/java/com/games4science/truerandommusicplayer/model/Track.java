package com.games4science.truerandommusicplayer.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tracks")
public class Track {

    @PrimaryKey
    @NonNull
    private final String uriString; // Use the URI as the unique ID

    @NonNull
    private final String name;

    @NonNull
    private final String artist;

    public Track(@NonNull String name, @NonNull String uriString, @NonNull String artist) {
        this.name = name;
        this.uriString = uriString;
        this.artist = artist;
    }

    @NonNull
    public String getUriString() { return uriString; }

    @NonNull
    public String getName() { return name; }

    @NonNull
    public String getArtist() { return artist; }



    // Helper to get actual Uri when needed for Media3
    public android.net.Uri getUri() {
        return android.net.Uri.parse(uriString);
    }
}
