package com.games4science.truerandommusicplayer.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tracks") // This tells Room to create a table
public class Track {

    @PrimaryKey(autoGenerate = true)
    private int id; // Needed for the DB to uniquely identify rows

    @NonNull
    private final String name;

    @NonNull
    private final String uriString; // Room prefers Strings over Uri objects

    public Track(@NonNull String name, @NonNull String uriString) {
        this.name = name;
        this.uriString = uriString;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }

    @NonNull
    public String getUriString() { return uriString; }

    // Helper to get actual Uri when needed for Media3
    public android.net.Uri getUri() {
        return android.net.Uri.parse(uriString);
    }
}
