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
    long createPlaylist(Playlist playlist);

    // Add a song to a playlist
    @Insert
    void addTrackToPlaylist(JoinPlaylistTrack join);

    @Insert
    void addTracksToPlaylist(List<JoinPlaylistTrack> joins);

    // --- QUERIES ---

    @Query("SELECT * FROM playlists")
    List<Playlist> getAllPlaylists();

    @Query("SELECT playlistId FROM playlists WHERE playlistName = :name LIMIT 1")
    long getPlaylistIdByName(String name);

    // Get all tracks belonging to a specific playlist name
    @Query("SELECT T.* FROM tracks T " +
            "INNER JOIN join_playlist_track J ON T.uriString = J.uriString " +
            "INNER JOIN playlists P ON P.playlistId = J.playlistId " +
            "WHERE P.playlistName = :pName")
    List<Track> getTracksForPlaylist(String pName);

    // --- UPDATES ---

    @Query("UPDATE playlists SET playlistName = :newName WHERE playlistName = :currentPlaylistName ")
    void renamePlaylist(String currentPlaylistName, String newName);

    // --- DELETES ---

    // Removes a track from a specific playlist (removes the link, not the file info)
    @Delete
    void removeTrackFromPlaylist(JoinPlaylistTrack join);

    @Query("DELETE FROM join_playlist_track WHERE joinId = (SELECT joinId FROM join_playlist_track WHERE playlistId = :pId AND uriString = :uStr LIMIT 1)")
    void removeOneInstance(int pId, String uStr);

    // Removes the track entirely from the library (cascades to all playlists)
    @Delete
    void deleteTrackCompletely(Track track);

    // Deletes the playlist (cascades to remove all track links automatically)
    @Delete
    void deletePlaylist(Playlist playlist);

    @Query("DELETE FROM playlists WHERE playlistName = :playlist ")
    void deletePlaylistByName(String playlist);

    // Quick helper to clear a playlist by name without having the Playlist object
    @Query("DELETE FROM join_playlist_track WHERE playlistId IN (SELECT playlistId FROM playlists WHERE playlistName = :pName)")
    void clearPlaylistByName(String pName);

    @Query("DELETE FROM join_playlist_track WHERE playlistId = :pId AND uriString = :uStr")
    void removeAllInstancesOfTrackFromPlaylist(int pId, String uStr);
}
