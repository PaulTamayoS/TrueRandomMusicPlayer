package com.games4science.truerandommusicplayer.api;

import android.content.Context;
import android.content.SharedPreferences;
import com.games4science.truerandommusicplayer.util.MyConstants;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static SubsonicApi subsonicApi;

    public static SubsonicApi getSubsonicApi(Context context) {
        if (subsonicApi == null) {
            SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS_SERVER_SETTINGS, Context.MODE_PRIVATE);
            String baseUrl = prefs.getString(MyConstants.PREFS_KEY_SERVER_URL, "");

            // Safety check: if no URL is set, we can't build the API
            if (baseUrl.isEmpty()) return null;

            // Ensure the URL ends with a slash / for Retrofit
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            subsonicApi = retrofit.create(SubsonicApi.class);
        }
        return subsonicApi;
    }

    // Call this if the user updates their server settings to force a rebuild
    public static void resetClient() {
        subsonicApi = null;
    }
}
