package com.games4science.truerandommusicplayer.api;

import retrofit2.Call;
import retrofit2.http.GET;

public interface SubsonicApi {
    @GET("rest/ping.view")
    Call<SubsonicResponse> ping();
}
