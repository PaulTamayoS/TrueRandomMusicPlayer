package com.games4science.truerandommusicplayer.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import com.games4science.truerandommusicplayer.util.MyConstants;
import com.games4science.truerandommusicplayer.util.MyUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TrackRepository {

    /**
     * Gets all available playlist names for the Spinner
     */
    public static List<String> getAllPlaylistNames(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
        // Get all keys (playlist names)
        List<String> names = new ArrayList<>(prefs.getAll().keySet());

        // Ensure default playlist always
        if (!names.contains(MyConstants.DEFAULT_PLAYLIST_NAME)) {
            names.add(0, MyConstants.DEFAULT_PLAYLIST_NAME); // Add to the start
        } else {
            // Optional: Move it to the start if it exists elsewhere
            names.remove(MyConstants.DEFAULT_PLAYLIST_NAME);
            names.add(0, MyConstants.DEFAULT_PLAYLIST_NAME);
        }
        return names;
    }

    public static void initializeEmptyPlaylist(Context context, String playlistName) {
        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(playlistName, "[]").apply(); // we put an empty array string
    }

    /**
     * Used for SINGLE file selection (User picked the file directly)
     */
    public static void saveTrack(Context context, String playlistName, Uri uri) {
        try {
            // Only persist if the URI supports it (picked via ACTION_OPEN_DOCUMENT)
            try {
                context.getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                // Not a persistable URI, ignore and continue
            }

            JSONObject trackJson = getTrackMetadata(context, uri);
            saveToPrefs(context, playlistName, trackJson);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Used for FOLDER selection (User picked the folder)
     */
    public static int saveTracksFromFolder(Context context,  String playlistName, Uri folderUri) {
        DocumentFile rootFolder = DocumentFile.fromTreeUri(context, folderUri);
        if (rootFolder == null || !rootFolder.isDirectory()) return 0;

        JSONArray allTracks = new JSONArray();
        int count = scanDirectoryRecursive(context, rootFolder, allTracks);

        // Save everything ONCE at the very end
        saveBatchToPrefs(context, playlistName, allTracks);
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
        if (artist == null || artist.isEmpty()) { artist = "X"; }

        JSONObject trackJson = new JSONObject();
        trackJson.put(MyConstants.STRING_URI, uri.toString());
        trackJson.put("title", title);
        trackJson.put("artist", artist);
        return trackJson;
    }

    private static void saveToPrefs(Context context, String playlistName, JSONObject trackJson) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
        JSONArray existing = new JSONArray(prefs.getString(playlistName, "[]"));
        existing.put(trackJson);
        prefs.edit().putString(playlistName, existing.toString()).commit();
    }

    private static void saveBatchToPrefs(Context context, String playlistName, JSONArray newBatch) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
            JSONArray existing = new JSONArray(prefs.getString(playlistName, "[]"));

            for (int i = 0; i < newBatch.length(); i++) {
                existing.put(newBatch.get(i));
            }

            prefs.edit().putString(playlistName, existing.toString()).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearTracks(Context context, String playlistName) {
        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(playlistName).commit();
    }

    public static List<MediaItem> getTracks(Context context, String playlistName) {
        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
        List<MediaItem> listOutput = new ArrayList<>();

        try {
            String jsonString = prefs.getString(playlistName, "[]");
            JSONArray array = new JSONArray(jsonString);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(obj.getString("title"))
                        .setArtist(obj.getString("artist"))
                        .build();

                MediaItem item = new MediaItem.Builder()
                        .setUri(Uri.parse(obj.getString(MyConstants.STRING_URI)))
                        .setMediaMetadata(metadata)
                        .build();

                listOutput.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listOutput;
    }

    public static int getTracksCount(Context context, String playlistName) {
        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
        try {
            String jsonString = prefs.getString(playlistName, "[]");
            JSONArray array = new JSONArray(jsonString);
            return array.length();
        } catch (Exception e) {
            return 0;
        }
    }

    public static void renamePlaylist(Context context, String oldName, String newName) {
        if (oldName.equals(newName))
            return;

        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
        String data = prefs.getString(oldName, "[]");

        prefs.edit()
                .putString(newName, data) // Copy data to new key
                .remove(oldName)          // Delete old key
                .apply();
    }

    public static List<JSONObject> getTrackObjects(Context context, String playlistName) {
        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
        List<JSONObject> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(playlistName, "[]"));
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getJSONObject(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void removeSingleTrack(Context context, String playlistName, String uriToRemove) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS, Context.MODE_PRIVATE);
            JSONArray existing = new JSONArray(prefs.getString(playlistName, "[]"));
            JSONArray updated = new JSONArray();

            for (int i = 0; i < existing.length(); i++) {
                JSONObject track = existing.getJSONObject(i);
                // Only keep tracks that DON'T match the URI we want to delete
                if (!track.getString(MyConstants.STRING_URI).equals(uriToRemove)) {
                    updated.put(track);
                }
            }

            prefs.edit().putString(playlistName, updated.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
