package com.games4science.truerandommusicplayer.model;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.games4science.truerandommusicplayer.util.MyConstants;

// We list all 3 entities and set the version to 1
@Database(entities = {Track.class, Playlist.class, JoinPlaylistTrack.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract LibraryDao libraryDao();
    private static volatile AppDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, MyConstants.ROOMDB_DBNAME)
                            .fallbackToDestructiveMigration() // during development to auto-wipe DB if you change the model
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
