package com.games4science.truerandommusicplayer.util;

public class MyConstants {
    private static final String PACKAGE = "com.games4science.truerandommusicplayer.";

    // 1. Storage Keys
    public static final String PREFS_TRACKS_REPO = "tracks_repo";
    public static final String DEFAULT_PLAYLIST_NAME = "My Library";

    // 2. Action IDs
    public static final String ACTION_LOAD_PLAYLIST = PACKAGE + "ACTION_LOAD_PLAYLIST";
    public static final String ACTION_TOGGLE_TRUE_RANDOM = PACKAGE + "ACTION_TOGGLE_TRUE_RANDOM";

    // 3. Intent Extra Keys
    public static final String EXTRA_PLAYLIST_NAME_TO_EDIT = PACKAGE + "EXTRA_PLAYLIST_TO_EDIT";
    public static final String EXTRA_PLAYLIST_NAME = PACKAGE + "EXTRA_PLAYLIST_NAME";

    // 4. JSON Model Keys
    public static final String JSON_KEY_URI = "uri";
    public static final String JSON_KEY_TITLE = "title";
    public static final String JSON_KEY_ARTIST = "artist";
}