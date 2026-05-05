package com.games4science.truerandommusicplayer.data;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import com.games4science.truerandommusicplayer.util.MyConstants;
import com.games4science.truerandommusicplayer.util.MyUtils;
import com.games4science.truerandommusicplayer.model.AppDatabase;
import com.games4science.truerandommusicplayer.model.JoinPlaylistTrack;
import com.games4science.truerandommusicplayer.model.LibraryDao;
import com.games4science.truerandommusicplayer.model.Playlist;
import com.games4science.truerandommusicplayer.model.Track;

import java.util.ArrayList;
import java.util.List;

public class TrackRepository {

    // Since Room is async, we need a way to tell the UI when data is ready.
    @FunctionalInterface
    public interface RepositoryCallback<T> {
        void onComplete(T result);
    }

    /**
     * Gets all available playlist names for the Spinner
     */
    public static void getAllPlaylistNames(Context context, RepositoryCallback<List<String>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            List<Playlist> playlists = dao.getAllPlaylists();
            List<String> names = new ArrayList<>();
            for (Playlist p : playlists) {
                if (!names.contains(p.playlistName)) {
                    names.add(p.playlistName);
                }
            }

            // Ensure default playlist exists in DB if it's missing
            if (!names.contains(MyConstants.DEFAULT_PLAYLIST_NAME)) {
                dao.createPlaylist(new Playlist(MyConstants.DEFAULT_PLAYLIST_NAME));
                names.add(0, MyConstants.DEFAULT_PLAYLIST_NAME);
            } else {
                // Move it to the start if it exists elsewhere
                names.remove(MyConstants.DEFAULT_PLAYLIST_NAME);
                names.add(0, MyConstants.DEFAULT_PLAYLIST_NAME);
            }

            callback.onComplete(names);
        });
    }

    public static void createPlaylist(Context context, String playlistName) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            dao.createPlaylist(new Playlist(playlistName));
        });
    }

    /**
     * Used for FILE(S) selection (User picked the file(s) directly)
     */
    public static void saveTracksFromFilesPicker(Context context, String playlistName, List<Uri> uris, RepositoryCallback<Integer> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = 0;
            try {
                LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
                long pId = dao.createPlaylist(new Playlist(playlistName));
                if (pId == -1) { // If createPlaylist returns -1, it already existed, so we find its ID
                    pId = dao.getPlaylistIdByName(playlistName);
                }

                List<JoinPlaylistTrack> joins = new ArrayList<>();
                for (Uri uri : uris) {
                    try {
                        // Only persist if the URI supports it (picked via ACTION_OPEN_DOCUMENT)
                        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        Track track = getTrackMetadata(context, uri);
                        dao.insertTrack(track);

                        joins.add(new JoinPlaylistTrack((int) pId, uri.toString()));
                        count++;
                    } catch (SecurityException ignored) {
                        // Not a persistable URI, ignore and continue
                    }
                }

                dao.addTracksToPlaylist(joins);

            } catch (Exception e) {
                e.printStackTrace();
            }

            callback.onComplete(count);
        });
    }

    /**
     * Used for FOLDER selection (User picked the folder)
     */
    public static void saveTracksFromFolder(Context context, String playlistName, Uri folderUri, RepositoryCallback<List<Track>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            DocumentFile rootFolder = DocumentFile.fromTreeUri(context, folderUri);
            if (rootFolder == null || !rootFolder.isDirectory()) return;

            List<Track> trackBatch = new ArrayList<>();
            scanDirectoryRecursive(context, rootFolder, trackBatch);

            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();

            // Bulk insert master tracks
            dao.insertTracks(trackBatch);

            // Get playlist ID
            long pId = dao.createPlaylist(new Playlist(playlistName));
            if (pId == -1)
                pId = dao.getPlaylistIdByName(playlistName);

            // Create joins
            List<JoinPlaylistTrack> joins = new ArrayList<>();
            for (Track t : trackBatch) {
                joins.add(new JoinPlaylistTrack((int) pId, t.getUriString()));
            }
            dao.addTracksToPlaylist(joins);

            callback.onComplete(trackBatch);
        });
    }

    private static void scanDirectoryRecursive(Context context, DocumentFile directory, List<Track> trackBatch) {
        DocumentFile[] files = directory.listFiles();

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                scanDirectoryRecursive(context, file, trackBatch); // RECURSION: If it's a folder, dive inside and add those results
            } else if (file.isFile() && file.getType() != null && file.getType().startsWith("audio/")) {
                try {
                    // Get metadata and add to our local batch
                    trackBatch.add(getTrackMetadata(context, file.getUri()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Track getTrackMetadata(Context context, Uri uri) throws Exception {
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

        return new Track(title, uri.toString(), artist);
    }

    public static void getTracks(Context context, String playlistName, RepositoryCallback<List<MediaItem>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            List<Track> dbTracks = dao.getTracksForPlaylist(playlistName);
            List<MediaItem> mediaItems = new ArrayList<>();

            for (Track t : dbTracks) {
                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(t.getName())
                        .setArtist(t.getArtist())
                        .build();

                mediaItems.add(new MediaItem.Builder()
                        .setUri(t.getUri())
                        .setMediaMetadata(metadata)
                        .build());
            }
            callback.onComplete(mediaItems);
        });
    }

    public static void getTracksAsModels(Context context, String playlistName, RepositoryCallback<List<Track>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            List<Track> dbTracks = dao.getTracksForPlaylist(playlistName);
            callback.onComplete(dbTracks);
        });
    }

    public static void getTracksCountByPlaylistName(Context context, String playlistName, RepositoryCallback<Integer> callback)
    {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            int count = dao.getTracksCountForPlaylist(playlistName);
            callback.onComplete(count);
        });
    }

    public static void removeSingleTrack(Context context, String playlistName, String uriToRemove) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            int pId = (int) dao.getPlaylistIdByName(playlistName);
            // This removes ONE instance (decreases weight by 1)
            dao.removeOneInstance(pId, uriToRemove);
        });
    }

    public static void deletePlaylistByName(Context context, String playlistName, RepositoryCallback<String> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            dao.deletePlaylistByName(playlistName);

            callback.onComplete(playlistName);
        });
    }

    public static void renamePlaylist(Context context, String currentPlaylistName, String newName, RepositoryCallback<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            try {
                // Check if the new name is already taken by a DIFFERENT playlist
                long existingId = dao.getPlaylistIdByName(newName);
                if (existingId != 0) {
                    callback.onComplete(false); // Name already exists!
                    return;
                }

                dao.renamePlaylist(currentPlaylistName, newName);
                callback.onComplete(true);
            } catch (Exception e) {
                callback.onComplete(false);
            }
        });
    }
}
