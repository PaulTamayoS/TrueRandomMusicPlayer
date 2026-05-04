package com.games4science.truerandommusicplayer.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface LibraryDao {

    // --- INSERTS ---

    // Insert a song. If it already exists, ignore it (to prevent duplicates)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTrack(Track track);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTracks(List<Track> tracks);

    // create a new playlist name
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertPlaylist(Playlist playlist);

    // Add a song to a playlist
    @Insert(onConflict = OnConflictStrategy.IGNORE) // TODO: actually, it is okay to have more than once the same song in the playlist
    void addTrackToPlaylist(JoinPlaylistTrack join);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addTracksToPlaylist(List<JoinPlaylistTrack> joins);

    // --- QUERIES ---

    // Get all tracks belonging to a specific playlist name
    @Query("SELECT T.* FROM tracks T " +
            "INNER JOIN join_playlist_track J ON T.uriString = J.uriString " +
            "INNER JOIN playlists P ON P.playlistId = J.playlistId " +
            "WHERE P.playlistName = :pName")
    List<Track> getTracksForPlaylist(String pName);

    // Get all playlists (to show in a list/menu)
    @Query("SELECT * FROM playlists")
    List<Playlist> getAllPlaylists();

    // --- DELETES ---

    // Removes a track from a specific playlist (removes the link, not the file info)
    @Delete
    void removeTrackFromPlaylist(JoinPlaylistTrack join);

    // Removes the track entirely from the library (cascades to all playlists)
    @Delete
    void deleteTrackCompletely(Track track);

    // Deletes the playlist (cascades to remove all track links automatically)
    @Delete
    void deletePlaylist(Playlist playlist);

    // Quick helper to clear a playlist by name without having the Playlist object
    @Query("DELETE FROM join_playlist_track WHERE playlistId IN (SELECT playlistId FROM playlists WHERE playlistName = :pName)")
    void clearPlaylistByName(String pName);
}
