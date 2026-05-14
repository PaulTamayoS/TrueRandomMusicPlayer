package com.games4science.truerandommusicplayer.data;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import com.games4science.truerandommusicplayer.api.RetrofitClient;
import com.games4science.truerandommusicplayer.api.SubsonicResponse;
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
    public static void getAllPlaylists(Context context, RepositoryCallback<List<Playlist>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            List<Playlist> playlists = dao.getAllPlaylists();

            //If there are 0 playlists, we create a default one
            if (playlists.isEmpty()) {
                dao.createPlaylist(new Playlist(MyConstants.DEFAULT_PLAYLIST_NAME));
                playlists = dao.getAllPlaylists();
            }

            callback.onComplete(playlists);
        });
    }

    public static void createPlaylist(Context context, String playlistName, RepositoryCallback<Long> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            long id = dao.createPlaylist(new Playlist(playlistName));

            callback.onComplete(id);
        });
    }

    public static void deletePlaylistById(Context context, long playlistId, RepositoryCallback<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            dao.deletePlaylistById(playlistId);
            callback.onComplete(true);
        });
    }

    public static void renamePlaylistById(Context context, long playlistId, String newName, RepositoryCallback<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            try {
                // Check if the new name is already taken by a DIFFERENT playlist
                long existingId = dao.getPlaylistIdByName(newName);
                if (existingId != 0 && existingId != playlistId) {
                    callback.onComplete(false); // Name already exists!
                    return;
                }

                dao.renamePlaylistById(playlistId, newName);
                callback.onComplete(true);
            } catch (Exception e) {
                callback.onComplete(false);
            }
        });
    }

    /**
     * Used for FILE(S) selection (User picked the file(s) directly)
     */
    public static void saveTracksFromFilesPicker(Context context, long playlistId, List<Uri> uris, RepositoryCallback<Integer> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = 0;
            try {
                LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
                List<JoinPlaylistTrack> joins = new ArrayList<>();
                for (Uri uri : uris) {
                    try {
                        // Only persist if the URI supports it (picked via ACTION_OPEN_DOCUMENT)
                        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        Track track = getTrackMetadata(context, uri);
                        dao.insertTrack(track);

                        joins.add(new JoinPlaylistTrack(playlistId, uri.toString()));
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
    public static void saveTracksFromFolder(Context context, long playlistId, Uri folderUri, RepositoryCallback<List<Track>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            DocumentFile rootFolder = DocumentFile.fromTreeUri(context, folderUri);
            if (rootFolder == null || !rootFolder.isDirectory()) {
                return;
            }

            List<Track> trackBatch = new ArrayList<>();
            scanDirectoryRecursive(context, rootFolder, trackBatch);

            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            dao.insertTracks(trackBatch);

            List<JoinPlaylistTrack> joins = new ArrayList<>();
            for (Track t : trackBatch) {
                joins.add(new JoinPlaylistTrack(playlistId, t.getUriString()));
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

    public static void getTracksAsListMediaItems(Context context, String playlistName, RepositoryCallback<List<MediaItem>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            List<Track> dbTracks = dao.getTracksForPlaylist(playlistName);
            List<MediaItem> mediaItems = new ArrayList<>();

            for (Track t : dbTracks) {
                Uri playbackUri;

                // check if subsonic track or local file
                if (t.getUriString().startsWith("subsonic://")) {
                    String songId = t.getUriString().replace("subsonic://", "");
                    String fullUrl = RetrofitClient.getStreamUrl(context, songId);
                    playbackUri = Uri.parse(fullUrl);
                } else {
                    playbackUri = t.getUri();
                }

                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(t.getName())
                        .setArtist(t.getArtist())
                        .build();

                mediaItems.add(new MediaItem.Builder()
                        .setUri(playbackUri)
                        .setMediaMetadata(metadata)
                        .build());
            }
            callback.onComplete(mediaItems);
        });
    }

    public static void getTracksAsModels(Context context, long playlistId, RepositoryCallback<List<Track>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            List<Track> dbTracks = dao.getTracksByPlaylistId(playlistId);
            callback.onComplete(dbTracks);
        });
    }

    public static void getTracksCountByPlaylistId(Context context, long playlistId, RepositoryCallback<Integer> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            int count = dao.getTracksCountByPlaylistId(playlistId);
            callback.onComplete(count);
        });
    }

    public static void removeSingleTrack(Context context, long playlistId, String uriToRemove) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            dao.removeOneInstance(playlistId, uriToRemove);
        });
    }

    public static void importSubsonicPlaylist(Context context, long localPlaylistId, List<SubsonicResponse.SongEntry> remoteSongs, RepositoryCallback<Integer> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            LibraryDao dao = AppDatabase.getDatabase(context).libraryDao();
            List<Track> tracksToInsert = new ArrayList<>();
            List<JoinPlaylistTrack> joins = new ArrayList<>();

            for (SubsonicResponse.SongEntry entry : remoteSongs) {
                // Create a URI starting with subsonic:// to identify it later
                String specialUri = "subsonic://" + entry.getId();

                Track track = new Track(entry.getTitle(), specialUri, entry.getArtist());
                tracksToInsert.add(track);

                joins.add(new JoinPlaylistTrack(localPlaylistId, specialUri));
            }

            dao.insertTracks(tracksToInsert);
            dao.addTracksToPlaylist(joins);

            callback.onComplete(joins.size());
    });
    }
}
