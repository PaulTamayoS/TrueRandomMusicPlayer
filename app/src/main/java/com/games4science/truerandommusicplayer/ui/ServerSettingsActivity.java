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
        String url = binding.editTextServerUrl.getText().toString().trim();
        String user = binding.editTextUsername.getText().toString().trim();
        String pass = binding.editTextPassword.getText().toString().trim();

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save current values to SharedPreferences first so the Interceptor can read them
        saveSettingsToPrefs();

        RetrofitClient.resetClient();
        SubsonicApi api = RetrofitClient.getSubsonicApi(this);
        if (api == null) {
            return;
        }

        // Make the call
        api.ping().enqueue(new retrofit2.Callback<SubsonicResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<SubsonicResponse> call, retrofit2.Response<SubsonicResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            SubsonicResponse.ResponseData data = response.body().getResponse();
                            if (data.isOk()) {
                                Toast.makeText(ServerSettingsActivity.this, "Connection Successful! Version: " + data.getVersion(), Toast.LENGTH_LONG).show();
                            } else {
                                String msg = data.getError() != null ? data.getError().getMessage() : "Unknown Error";
                                Toast.makeText(ServerSettingsActivity.this, "Server Error: " + msg, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(ServerSettingsActivity.this, "HTTP Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<SubsonicResponse> call, Throwable t) {
                        Toast.makeText(ServerSettingsActivity.this, "Network Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUrlSecurity(String url) {
        if (url.toLowerCase().startsWith("http://")) {
            binding.tvHttpWarning.setVisibility(View.VISIBLE);
        } else {
            binding.tvHttpWarning.setVisibility(View.GONE); }
    }
}
