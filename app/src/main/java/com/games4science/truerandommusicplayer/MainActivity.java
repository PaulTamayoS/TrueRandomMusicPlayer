package com.games4science.truerandommusicplayer;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.games4science.truerandommusicplayer.data.TrackRepository;
import com.games4science.truerandommusicplayer.databinding.ActivityMainBinding;
import com.games4science.truerandommusicplayer.player.MusicService;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_AUDIO = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestNotificationPermission();

        findViewById(R.id.btnPickMusic).setOnClickListener(v -> openPicker());
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK) {

            if (data.getData() != null) {
                TrackRepository.saveTrack(this, data.getData());
            } else if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    TrackRepository.saveTrack(this, uri);
                }
            }

            startService(new Intent(this, MusicService.class));
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    101
            );
        }
    }
}