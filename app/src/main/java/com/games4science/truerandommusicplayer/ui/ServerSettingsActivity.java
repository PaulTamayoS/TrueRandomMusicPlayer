package com.games4science.truerandommusicplayer.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.games4science.truerandommusicplayer.api.RetrofitClient;
import com.games4science.truerandommusicplayer.api.SubsonicApi;
import com.games4science.truerandommusicplayer.api.SubsonicResponse;
import com.games4science.truerandommusicplayer.databinding.ActivityServerSettingsBinding;
import com.games4science.truerandommusicplayer.util.MyConstants;
import com.games4science.truerandommusicplayer.util.MyUtils;

import java.util.List;
import java.util.function.Consumer;

public class ServerSettingsActivity extends AppCompatActivity {

    private ActivityServerSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityServerSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadSettings();

        binding.editTextServerUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkUrlSecurity(s.toString());
            }
        });

        binding.btnSaveServer.setOnClickListener(v -> saveAndExit());
        binding.btnTestConnection.setOnClickListener(v -> testConnection());
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(MyConstants.PREFS_SERVER_SETTINGS, MODE_PRIVATE);
        String url = prefs.getString(MyConstants.PREFS_KEY_SERVER_URL, "");
        binding.editTextServerUrl.setText(url);
        binding.editTextUsername.setText(prefs.getString(MyConstants.PREFS_KEY_SERVER_USER, ""));
        binding.editTextPassword.setText(prefs.getString(MyConstants.PREFS_KEY_SERVER_PASSWORD, ""));

        checkUrlSecurity(url);
    }

    private void checkUrlSecurity(String url) {
        if (url.toLowerCase().startsWith("http://")) {
            binding.tvHttpWarning.setVisibility(View.VISIBLE);
        } else {
            binding.tvHttpWarning.setVisibility(View.GONE); }
    }

    private void saveAndExit() {
        saveSettingsToPrefs();
        finish();
    }

    private void saveSettingsToPrefs()
    {
        String url = binding.editTextServerUrl.getText().toString().trim();
        String user = binding.editTextUsername.getText().toString().trim();
        String pass = binding.editTextPassword.getText().toString().trim();

        getSharedPreferences(MyConstants.PREFS_SERVER_SETTINGS, MODE_PRIVATE)
                .edit()
                .putString(MyConstants.PREFS_KEY_SERVER_URL, url)
                .putString(MyConstants.PREFS_KEY_SERVER_USER, user)
                .putString(MyConstants.PREFS_KEY_SERVER_PASSWORD, pass)
                .apply();
    }

    private void testConnection() {
        SubsonicApi api = getValidatedApi();

        if (api == null) {
            return;
        }

        executeRequest(api.ping(), data -> {
            Toast.makeText(this, "Connection Successful! Version: " + data.getVersion(), Toast.LENGTH_LONG).show();
        });
    }

    private void getPlaylists() {
        SubsonicApi api = getValidatedApi();

        if (api == null) {
            return;
        }

        executeRequest(api.getPlaylists(), data -> {
            if (data.getPlaylists() != null && data.getPlaylists().getPlaylist() != null)
            {
                List<SubsonicResponse.Playlist> remotePlaylists = data.getPlaylists().getPlaylist();
                Toast.makeText(this, "Success! Found " + remotePlaylists.size() + " playlists.", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "Connected, but you have 0 playlists on the server.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchTracks(String playlistId) {
        SubsonicApi api = RetrofitClient.getSubsonicApi(this);

        if (api == null) {
            return;
        }

        executeRequest(api.getPlaylist(playlistId), data -> {
            if (data.getPlaylist() != null && data.getPlaylist().getEntries() != null)
            {
                java.util.List<SubsonicResponse.SongEntry> songs = data.getPlaylist().getEntries();

                for (SubsonicResponse.SongEntry song : songs) {
                    // Now you have the song title, artist, and ID!
                    android.util.Log.d("Subsonic", "Found song: " + song.getTitle());
                }

                Toast.makeText(this, "Fetched " + songs.size() + " songs!", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "Playlist has 0 songs!!!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private SubsonicApi getValidatedApi() {
        String url = binding.editTextServerUrl.getText().toString().trim();
        String user = binding.editTextUsername.getText().toString().trim();
        String pass = binding.editTextPassword.getText().toString().trim();

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return null;
        }

        // Save current values to SharedPreferences first so the Interceptor can read them
        saveSettingsToPrefs();

        RetrofitClient.resetClient();
        SubsonicApi api = RetrofitClient.getSubsonicApi(this);

        return api;
    }

    private void executeRequest(retrofit2.Call<SubsonicResponse> call, Consumer<SubsonicResponse.ResponseData> listener) {
        call.enqueue(new retrofit2.Callback<SubsonicResponse>() {
            @Override
            public void onResponse(retrofit2.Call<SubsonicResponse> call, retrofit2.Response<SubsonicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    SubsonicResponse.ResponseData data = response.body().getResponse();

                    if (data.isOk()) {
                        listener.accept(data);
                    } else {
                        String msg = data.getError() != null ? data.getError().getMessage() : "Unknown Error";
                        Toast.makeText(ServerSettingsActivity.this, "Server Error: " + msg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ServerSettingsActivity.this, "HTTP Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SubsonicResponse> call, Throwable t) {
                Toast.makeText(ServerSettingsActivity.this, "Network Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
