package com.games4science.truerandommusicplayer.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface LibraryDao {

    // Insert a song. If it already exists, ignore it (to prevent duplicates)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTrack(Track track);

    // create a new playlist name
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertPlaylist(Playlist playlist);

    // Add a song to a playlist
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addTrackToPlaylist(JoinPlaylistTrack ref);

    // Get all tracks belonging to a specific playlist name
    @Query("SELECT tracks.* FROM tracks " +
            "INNER JOIN join_playlist_track ON tracks.uriString = join_playlist_track.uriString " +
            "INNER JOIN playlists ON playlists.playlistId = join_playlist_track.playlistId " +
            "WHERE playlists.playlistName = :pName")
    List<Track> getTracksForPlaylist(String pName);

//
//    // Insert multiple songs at once (useful for scanning a whole folder)
//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    void insertAll(List<Track> tracks);

    // Delete a specific track from the playlist
    @Delete
    void deleteTrackFromPlaylist(JoinPlaylistTrack ref);

    // Delete a playlist
    @Delete
    void deletePlaylist(Playlist playlist);
}
