package com.games4science.truerandommusicplayer.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface SubsonicApi {
    @GET("rest/ping.view")
    Call<SubsonicResponse> ping(
            @Query("u") String username,
            @Query("t") String token,
            @Query("s") String salt,
            @Query("v") String version,
            @Query("c") String clientName,
            @Query("f") String format
    );
}
