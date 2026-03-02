package com.games4science.truerandommusicplayer.model;

import android.net.Uri;

public class Track {

    private final String name;
    private final Uri uri;

    public Track(String name, Uri uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() { return name; }
    public Uri getUri() { return uri; }
}
