package com.games4science.truerandommusicplayer.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;
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

    /**
     * Used for SINGLE file selection (User picked the file directly)
     */
    public static void saveTrack(Context context, Uri uri) {
        try {
            // Only persist if the URI supports it (picked via ACTION_OPEN_DOCUMENT)
            try {
                context.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                // Not a persistable URI, ignore and continue
            }

            JSONObject trackJson = getTrackMetadata(context, uri);
            saveToPrefs(context, trackJson);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Used for FOLDER selection (User picked the folder)
     */
    public static int saveTracksFromFolder(Context context, Uri folderUri) {
        DocumentFile rootFolder = DocumentFile.fromTreeUri(context, folderUri);
        if (rootFolder == null || !rootFolder.isDirectory()) return 0;

        JSONArray allTracks = new JSONArray();
        int count = scanDirectoryRecursive(context, rootFolder, allTracks);

        // Save everything ONCE at the very end
        saveBatchToPrefs(context, allTracks);
        return count;
    }

    private static int scanDirectoryRecursive(Context context, DocumentFile directory, JSONArray allTracks) {
        int addedCount = 0;
        DocumentFile[] files = directory.listFiles();

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                // RECURSION: If it's a folder, dive inside and add those results to our count
                addedCount += scanDirectoryRecursive(context, file, allTracks);
            } else if (file.isFile() && file.getType() != null && file.getType().startsWith("audio/")) {
                try {
                    // Get metadata and add to our local batch
                    JSONObject trackJson = getTrackMetadata(context, file.getUri());
                    allTracks.put(trackJson);
                    addedCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return addedCount;
    }

    // --- HELPER METHODS TO CLEAN UP CODE ---
    private static JSONObject getTrackMetadata(Context context, Uri uri) throws Exception {
        String title = "Unknown Song";
        String artist = "Unknown Artist";
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(context, uri);
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        } catch (Exception ignored) {
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }

        // fallback title/artist if metadata is missing
        if (title == null || title.isEmpty()) { title = MyUtils.getFileNameFromUri(context, uri); }
        if (artist == null || artist.isEmpty()) { artist = "Unknown Artist"; }

        JSONObject trackJson = new JSONObject();
        trackJson.put("uri", uri.toString());
        trackJson.put("title", title);
        trackJson.put("artist", artist);
        return trackJson;
    }

    private static void saveToPrefs(Context context, JSONObject trackJson) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_JSON_TRACKS, "[]");
        JSONArray array = new JSONArray(jsonString);
        array.put(trackJson);
        prefs.edit().putString(KEY_JSON_TRACKS, array.toString()).commit();
    }

    private static void saveBatchToPrefs(Context context, JSONArray newBatch) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray existing = new JSONArray(prefs.getString(KEY_JSON_TRACKS, "[]"));

            for (int i = 0; i < newBatch.length(); i++) {
                existing.put(newBatch.get(i));
            }

            prefs.edit().putString(KEY_JSON_TRACKS, existing.toString()).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearTracks(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_JSON_TRACKS).commit();
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
