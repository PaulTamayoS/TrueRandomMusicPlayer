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

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();

        initializeController();

        binding.btnPickMusic.setOnClickListener(v -> openPicker());

        binding.btnPlay.setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.play();
            }
        });

        binding.btnPause.setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.pause();
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.seekToNextMediaItem();
            }
        });

        binding.btnPrevious.setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.seekToPreviousMediaItem();
            }
        });
    }

    private void initializeController() {

        SessionToken sessionToken =
                new SessionToken(
                        this,
                        new ComponentName(this, MusicService.class)
                );

        controllerFuture =
                new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
            } catch (ExecutionException | InterruptedException e) {
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

                                // Start the MediaSessionService
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

        if (mediaController != null) {
            mediaController.release();
            mediaController = null;
        }
    }
}