package com.games4science.truerandommusicplayer.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import com.games4science.truerandommusicplayer.util.MyUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TrackRepository {

    private static final String PREFS = "tracks_repo";
    private static final String KEY_JSON_TRACKS = "json_tracks"; // Store everything in one JSON string

    public static void saveTrack(Context context, Uri uri) {
        try {
            context.getContentResolver().takePersistableUriPermission( uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 1. Get metadata immediately when saving
            String title = "Unknown Song";
            String artist = "Unknown Artist";
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(context, uri);
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            } catch (Exception ignored) {}
            finally { retriever.release(); }

            // fallback title/artist if metadata is missing
            if (title == null || title.isEmpty()) { title = MyUtils.getFileNameFromUri(context, uri); }
            if (artist == null || artist.isEmpty()) { artist = "Unknown Artist"; }

            // 2. Load existing list, add new track, and save back
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String jsonString = prefs.getString(KEY_JSON_TRACKS, "[]");
            JSONArray array = new JSONArray(jsonString);

            JSONObject trackJson = new JSONObject();
            trackJson.put("uri", uri.toString());
            trackJson.put("title", title);
            trackJson.put("artist", artist);

            array.put(trackJson);
            prefs.edit().putString(KEY_JSON_TRACKS, array.toString()).apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearTracks(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_JSON_TRACKS).apply();
    }

    public static List<MediaItem> getTracks(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<MediaItem> listOutput = new ArrayList<>();

        try {
            String jsonString = prefs.getString(KEY_JSON_TRACKS, "[]");
            JSONArray array = new JSONArray(jsonString);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(obj.getString("title"))
                        .setArtist(obj.getString("artist"))
                        .build();

                MediaItem item = new MediaItem.Builder()
                        .setUri(Uri.parse(obj.getString("uri")))
                        .setMediaMetadata(metadata)
                        .build();

                listOutput.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listOutput;
    }
}
