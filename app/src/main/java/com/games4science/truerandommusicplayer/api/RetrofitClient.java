package com.games4science.truerandommusicplayer.api;

import android.content.Context;
import android.content.SharedPreferences;
import com.games4science.truerandommusicplayer.util.MyConstants;
import com.games4science.truerandommusicplayer.util.MyUtils;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static SubsonicApi subsonicApi;

    public static SubsonicApi getSubsonicApi(Context context) {
        if (subsonicApi == null) {
            SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS_SERVER_SETTINGS, Context.MODE_PRIVATE);
            String baseUrl = prefs.getString(MyConstants.PREFS_KEY_SERVER_URL, "");

            // Safety check: if no URL is set, we can't build the API
            if (baseUrl.isEmpty()) {
                return null;
            }
            // Ensure the URL ends with a slash / for Retrofit
            if (baseUrl.endsWith("/") == false) {
                baseUrl += "/";
            }

            // Create the Auth Interceptor
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        HttpUrl originalHttpUrl = original.url();

                        // Retrieve current credentials from SharedPreferences
                        String user = prefs.getString(MyConstants.PREFS_KEY_SERVER_USER, "");
                        String pass = prefs.getString(MyConstants.PREFS_KEY_SERVER_PASSWORD, "");

                        String salt = String.valueOf(System.currentTimeMillis());
                        String token = MyUtils.generateMd5Token(pass, salt);

                        // Build the new URL with all Subsonic requirements
                        HttpUrl url = originalHttpUrl.newBuilder()
                                .addQueryParameter("u", user)
                                .addQueryParameter("t", token)
                                .addQueryParameter("s", salt)
                                .addQueryParameter("v", "1.16.1")
                                .addQueryParameter("c", "TrueRandomMusicPlayer")
                                .addQueryParameter("f", "json")
                                .build();

                        // Rebuild the request with the new URL
                        Request request = original.newBuilder().url(url).build();
                        return chain.proceed(request);
                    })
                    .build();

            // Build Retrofit using the OkHttpClient
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            subsonicApi = retrofit.create(SubsonicApi.class);
        }
        return subsonicApi;
    }

    public static void resetClient() {
        subsonicApi = null;
    }
}
