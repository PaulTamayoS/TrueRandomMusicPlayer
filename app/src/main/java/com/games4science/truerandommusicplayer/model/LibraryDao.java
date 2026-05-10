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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long createPlaylist(Playlist playlist);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTrack(Track track);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTracks(List<Track> tracks);

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

    @Query("SELECT T.* FROM tracks T " +
            "INNER JOIN join_playlist_track J ON T.uriString = J.uriString " +
            "WHERE J.playlistId = :pId")
    List<Track> getTracksByPlaylistId(long pId);

    @Query("SELECT COUNT(*) FROM join_playlist_track WHERE playlistId = :pId")
    int getTracksCountByPlaylistId(long pId);

    // --- UPDATES ---

    @Query("UPDATE playlists SET playlistName = :newName WHERE playlistId = :pId")
    void renamePlaylistById(long pId, String newName);

    // --- DELETES ---

    // Removes a track from a specific playlist (removes the link, not the file info)
    @Delete
    void removeTrackFromPlaylist(JoinPlaylistTrack join);

    @Query("DELETE FROM join_playlist_track WHERE joinId = (SELECT joinId FROM join_playlist_track WHERE playlistId = :pId AND uriString = :uStr LIMIT 1)")
    void removeOneInstance(long pId, String uStr);

    // Removes the track entirely from the library (cascades to all playlists)
    @Delete
    void deleteTrackCompletely(Track track);

    @Query("DELETE FROM playlists WHERE playlistId = :pId")
    void deletePlaylistById(long pId);

    // Quick helper to clear a playlist by name without having the Playlist object
    @Query("DELETE FROM join_playlist_track WHERE playlistId IN (SELECT playlistId FROM playlists WHERE playlistName = :pName)")
    void clearPlaylistByName(String pName);

    @Query("DELETE FROM join_playlist_track WHERE playlistId = :pId AND uriString = :uStr")
    void removeAllInstancesOfTrackFromPlaylist(long pId, String uStr);
}
