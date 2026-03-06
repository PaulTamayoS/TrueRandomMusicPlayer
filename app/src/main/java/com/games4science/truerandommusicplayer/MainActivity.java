package com.games4science.truerandommusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.games4science.truerandommusicplayer.data.TrackRepository;
import com.games4science.truerandommusicplayer.databinding.ActivityMainBinding;
import com.games4science.truerandommusicplayer.player.MusicService;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final int REQUEST_NOTIF = 100;

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();

        connectToService();

        binding.btnAddMusic.setOnClickListener(v -> openPicker());

        binding.btnClear.setOnClickListener(v -> {
            TrackRepository.clearTracks(this);
            Intent intent = new Intent(this, MusicService.class);
            intent.setAction("LOAD_PLAYLIST");
            ContextCompat.startForegroundService(this, intent);
        });

        binding.btnPlay.setOnClickListener(v -> {
            if (controller  != null) controller.play();
        });

        binding.btnPause.setOnClickListener(v -> {
            if (controller != null) controller.pause();
        });

        binding.btnNext.setOnClickListener(v -> {
            if (controller != null) controller.seekToNextMediaItem();
        });

        binding.btnPrevious.setOnClickListener(v -> {
            if (controller != null) controller.seekToPreviousMediaItem();
        });

        binding.btnStop.setOnClickListener(v -> {
            if (controller != null)
            {
                controller.stop();
                controller.seekTo(0);
            }
        });
    }

    private void connectToService() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));

        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        openPickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> openPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                            Intent data = result.getData();
                            boolean addedAny = false;
                            int countAddedTracks = 0;

                            if (data.getData() != null)
                            {
                                TrackRepository.saveTrack(this, data.getData());
                                addedAny = true;
                                countAddedTracks++;
                            }
                            else if (data.getClipData() != null)
                            {
                                for (int i = 0; i < data.getClipData().getItemCount(); i++)
                                {
                                    Uri uri = data.getClipData().getItemAt(i).getUri();
                                    TrackRepository.saveTrack(this, uri);
                                    addedAny = true;
                                    countAddedTracks++;
                                }
                            }

                            if (addedAny) {
                                Toast.makeText(this, "Tracks added! Total = " + countAddedTracks, Toast.LENGTH_SHORT).show();

                                // Start the MediaSessionService
                                Intent serviceIntent = new Intent(this, MusicService.class);
                                serviceIntent.setAction("LOAD_PLAYLIST");

                                ContextCompat.startForegroundService(this, serviceIntent);
                            }
                        }
                    }
            );

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions( new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIF );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIF) {
            if (grantResults.length > 0 && grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Notification permission denied. Playback notification may not appear.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (controller != null) {
            controller.release();
            controller = null;
        }
    }
}