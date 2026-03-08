package com.games4science.truerandommusicplayer.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrackRepository {

    private static final String PREFS = "tracks_repo";
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

    public static void clearTracks(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_URIS).apply();
    }

    public static List<MediaItem> getTracks(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<MediaItem> listOutput = new ArrayList<>();

        Set<String> setUris = new HashSet<>(prefs.getStringSet(KEY_URIS, new HashSet<>()));


        for (String s : setUris) {

            Uri uri = Uri.parse(s);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            String title = null;
            String artist = null;
            String album = null;

            try {

                retriever.setDataSource(context, uri);

                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    retriever.release();
                }
                catch (Exception ignored) {}
            }

            // fallback title if metadata missing
            if (title == null) {
                title = getFileNameFromUri(context, uri);
            }

            if (artist == null) {
                artist = "No artist";
            }

            if (album == null) {
                album = "No album";
            }

            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build();

            MediaItem createdMediaItem = new MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(metadata)
                    .build();


            listOutput.add(createdMediaItem);
        }
        return listOutput;
    }

    private static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = null;

        // If it's a content URI (the most common for selected files)
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // If it's a file URI or the content query failed
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
            if (fileName != null && fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
        }

        return fileName != null ? fileName.replace("%20", " ") : "Unknown title";
    }
}
