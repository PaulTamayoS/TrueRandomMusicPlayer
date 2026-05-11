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

        public String getStatus() { return status; }
        public String getVersion() { return version; }
        public Error getError() { return error; }
        public boolean isOk() { return "ok".equals(status); }
    }

    public static class Error {
        private int code;
        private String message;

        public int getCode() { return code; }
        public String getMessage() { return message; }
    }
}
