package com.games4science.truerandommusicplayer.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.games4science.truerandommusicplayer.ui.ServerSettingsActivity;
import com.games4science.truerandommusicplayer.util.MyConstants;
import com.games4science.truerandommusicplayer.util.MyUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
            if (baseUrl.isEmpty() || baseUrl.toLowerCase().startsWith("http") == false) {
                return null;
            }
            // Ensure the URL ends with a slash / for Retrofit
            if (baseUrl.endsWith("/") == false) {
                baseUrl += "/";
            }

            try {
                // Create the Auth Interceptor
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
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
            } catch (Exception e) {
                return null;
            }
        }
        return subsonicApi;
    }

    public static void resetClient() {
        subsonicApi = null;
    }

    public static void executeRequest(Context callingContext, retrofit2.Call<SubsonicResponse> call, Consumer<SubsonicResponse.ResponseData> listener) {
        call.enqueue(new retrofit2.Callback<SubsonicResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SubsonicResponse> call, retrofit2.Response<SubsonicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    SubsonicResponse.ResponseData data = response.body().getResponse();

                    if (data.isOk()) {
                        listener.accept(data);
                    } else {
                        String msg = data.getError() != null ? data.getError().getMessage() : "Unknown Error";
                        Toast.makeText(callingContext, "Server Error: " + msg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(callingContext, "HTTP Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SubsonicResponse> call, Throwable t) {
                Toast.makeText(callingContext, "Network Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public static String getStreamUrl(Context context, String songId) {
        SharedPreferences prefs = context.getSharedPreferences(MyConstants.PREFS_SERVER_SETTINGS, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(MyConstants.PREFS_KEY_SERVER_URL, "");
        String user = prefs.getString(MyConstants.PREFS_KEY_SERVER_USER, "");
        String pass = prefs.getString(MyConstants.PREFS_KEY_SERVER_PASSWORD, "");

        if (baseUrl.isEmpty() || songId == null) return null;
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String salt = String.valueOf(System.currentTimeMillis());
        String token = MyUtils.generateMd5Token(pass, salt);

        // Build the stream.view URL with all required auth parameters
        return baseUrl + "rest/stream.view" +
                "?id=" + songId +
                "&u=" + user +
                "&t=" + token +
                "&s=" + salt +
                "&v=1.16.1" +
                "&c=TrueRandomMusicPlayer" +
                "&f=json";
    }
}
