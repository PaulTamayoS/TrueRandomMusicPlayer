package com.games4science.truerandommusicplayer.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrackRepository {

    private static final String PREFS = "tracks";
    private static final String KEY_URIS = "uris";

    public static void saveTrack(Context context, Uri uri) {
        context.getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_URIS, new HashSet<>()));
        set.add(uri.toString());
        prefs.edit().putStringSet(KEY_URIS, set).apply();
    }

    public static List<Uri> getTracks(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_URIS, new HashSet<>());
        List<Uri> list = new ArrayList<>();
        for (String s : set) {
            list.add(Uri.parse(s));
        }
        return list;
    }
}
