package com.games4science.truerandommusicplayer.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SubsonicApi {
    @GET("rest/ping.view")
    Call<SubsonicResponse> ping();

    @GET("rest/getPlaylists.view")
    Call<SubsonicResponse> getPlaylists();

    @GET("rest/getPlaylist.view")
    Call<SubsonicResponse> getPlaylist(@Query("id") String playlistId);
}
