package com.games4science.truerandommusicplayer.api;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

@Keep
public class SubsonicResponse {

    @SerializedName("subsonic-response")
    private ResponseData response;

    public ResponseData getResponse() {
        return response;
    }

    @Keep
    public static class ResponseData {
        private String status; // "ok" or "failed"
        private String version;
        private Error error;

        private PlaylistList playlists;
        private Playlist playlist;

        public String getStatus() { return status; }
        public boolean isOk() { return "ok".equals(status); }
        public String getVersion() { return version; }
        public Error getError() { return error; }
        public PlaylistList getPlaylists() { return playlists; }
        public Playlist getPlaylist() { return playlist; }
    }

    @Keep
    public static class PlaylistList {
        @SerializedName("playlist")
        private java.util.List<Playlist> playlist;

        public java.util.List<Playlist> getPlaylist() { return playlist; }
    }

    @Keep
    public static class Playlist {
        private String id;
        private String name;
        private int songCount;
        @SerializedName("entry") // Subsonic calls the list of songs "entry"
        private java.util.List<SongEntry> entries;

        public String getId() { return id; }
        public String getName() { return name; }
        public int getSongCount() { return songCount; }
        public java.util.List<SongEntry> getEntries() { return entries; }
    }

    @Keep
    public static class SongEntry {
        private String id;
        private String title;
        private String artist;
        private int duration; // In seconds private

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public int getDuration() { return duration; }
    }

    @Keep
    public static class Error {
        private int code;
        private String message;

        public int getCode() { return code; }
        public String getMessage() { return message; }
    }
}
