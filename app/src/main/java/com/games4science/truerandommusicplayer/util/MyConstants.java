package com.games4science.truerandommusicplayer.util;

public class MyConstants {
    private static final String PACKAGE = "com.games4science.truerandommusicplayer.";

    // Storage Keys
    public static final String PREFS_REPO_PLAYER = "PREFS_REPO_PLAYER";
    public static final String PREFS_KEY_LAST_PLAYLIST = "PREFS_KEY_LAST_PLAYLIST";
    public static final String DEFAULT_PLAYLIST_NAME = "My Library";

    public static final String ROOMDB_DBNAME = "TRMP_DB";

    public static final String PREFS_SERVER_SETTINGS = "PREFS_SERVER_SETTINGS";
    public static final String PREFS_KEY_SERVER_URL = "PREFS_KEY_SERVER_URL";
    public static final String PREFS_KEY_SERVER_USER = "PREFS_KEY_SERVER_USER";
    public static final String PREFS_KEY_SERVER_PASSWORD = "PREFS_KEY_SERVER_PASSWORD";

    // Action IDs
    public static final String ACTION_LOAD_PLAYLIST = PACKAGE + "ACTION_LOAD_PLAYLIST";
    public static final String ACTION_TOGGLE_TRUE_RANDOM = PACKAGE + "ACTION_TOGGLE_TRUE_RANDOM";

    // Intent Extra Keys
    public static final String EXTRA_PLAYLIST_ID_TO_EDIT = PACKAGE + "EXTRA_PLAYLIST_ID_TO_EDIT";
    public static final String EXTRA_PLAYLIST_NAME_TO_EDIT = PACKAGE + "EXTRA_PLAYLIST_NAME_TO_EDIT";
    public static final String EXTRA_PLAYLIST_NAME = PACKAGE + "EXTRA_PLAYLIST_NAME";
    public static final String EXTRA_STATE_TOGGLE_TRUE_RANDOM = PACKAGE + "STATE_TOGGLE_TRUE_RANDOM";
}