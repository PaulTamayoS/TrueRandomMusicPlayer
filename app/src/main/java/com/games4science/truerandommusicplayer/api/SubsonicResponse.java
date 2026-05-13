package com.games4science.truerandommusicplayer.api;

import com.google.gson.annotations.SerializedName;

public class SubsonicResponse {

    @SerializedName("subsonic-response")
    private ResponseData response;

    public ResponseData getResponse() {
        return response;
    }

    public static class ResponseData {
        private String status; // "ok" or "failed"
        private String version;
        private Error error;

        private PlaylistList playlists;

        public String getStatus() { return status; }
        public String getVersion() { return version; }
        public Error getError() { return error; }
        public PlaylistList getPlaylists() { return playlists; }
        public boolean isOk() { return "ok".equals(status); }
    }

    public static class PlaylistList {
        @SerializedName("playlist")
        private java.util.List<Playlist> playlist;

        public java.util.List<Playlist> getPlaylist() { return playlist; }
    }

    public static class Playlist {
        private String id;
        private String name;
        private int songCount;
        private String owner;

        public String getId() { return id; }
        public String getName() { return name; }
        public int getSongCount() { return songCount; }
        public String getOwner() { return owner; }
    }

    public static class Error {
        private int code;
        private String message;

        public int getCode() { return code; }
        public String getMessage() { return message; }
    }
}
