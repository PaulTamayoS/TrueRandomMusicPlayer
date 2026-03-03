package com.games4science.truerandommusicplayer;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.games4science.truerandommusicplayer.data.TrackRepository;
import com.games4science.truerandommusicplayer.databinding.ActivityMainBinding;
import com.games4science.truerandommusicplayer.player.MusicService;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final int REQUEST_NOTIF = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();

        binding.btnPickMusic.setOnClickListener(v -> openPicker());

        binding.btnPlay.setOnClickListener(v -> sendCommand(MusicService.ACTION_PLAY));

        binding.btnPause.setOnClickListener(v -> sendCommand(MusicService.ACTION_PAUSE));

        binding.btnNext.setOnClickListener(v -> sendCommand(MusicService.ACTION_NEXT));

        binding.btnPrevious.setOnClickListener(v -> sendCommand(MusicService.ACTION_PREVIOUS));

        binding.btnStop.setOnClickListener(v -> sendCommand(MusicService.ACTION_STOP));
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        openPickerLauncher.launch(intent);
    }

    private void sendCommand(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        ContextCompat.startForegroundService(this, intent);
    }

    private final ActivityResultLauncher<Intent> openPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                            Intent data = result.getData();
                            boolean addedAny = false;

                            if (data.getData() != null) {
                                TrackRepository.saveTrack(this, data.getData());
                                addedAny = true;
                            } else if (data.getClipData() != null) {
                                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                    Uri uri = data.getClipData().getItemAt(i).getUri();
                                    TrackRepository.saveTrack(this, uri);
                                    addedAny = true;
                                }
                            }

                            if (addedAny) {
                                Toast.makeText(this, "Tracks added!", Toast.LENGTH_SHORT).show();

                                // Launch the music service
                                ContextCompat.startForegroundService(
                                        this,
                                        new Intent(this, MusicService.class)
                                );
                            }
                        }
                    }
            );

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIF
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIF) {
            if (grantResults.length > 0 && grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Notification permission denied! Music notifications may not appear.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}