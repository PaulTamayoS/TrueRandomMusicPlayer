package com.games4science.truerandommusicplayer.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface TrackDao {

    // Get all tracks for a specific playlist (like "favorites" or "main")
    // We will use the playlistName to separate your "Random" lists
    @Query("SELECT * FROM tracks")
    List<Track> getAllTracks();

    // Insert a song. If it already exists, ignore it (to prevent duplicates)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Track track);

    // Insert multiple songs at once (useful for scanning a whole folder)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Track> tracks);

    // Delete a specific track by its URI
    @Query("DELETE FROM tracks WHERE uriString = :uriStr")
    void deleteByUri(String uriStr);

    // Clear the whole list
    @Query("DELETE FROM tracks")
    void deleteAll();
}
