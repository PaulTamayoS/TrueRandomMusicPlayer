package com.games4science.truerandommusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.games4science.truerandommusicplayer.data.TrackRepository;
import com.games4science.truerandommusicplayer.databinding.ActivityMainBinding;
import com.games4science.truerandommusicplayer.player.MusicService;
import com.games4science.truerandommusicplayer.util.MyUtils;
import com.google.common.util.concurrent.ListenableFuture;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final int REQUEST_NOTIF = 100;

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;

    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private boolean isUserInteractingWithSeekingBar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();

        connectToService();

        binding.btnAddMusic.setOnClickListener(v -> openPicker());
        binding.btnClear.setOnClickListener(v ->  OnClickBtnClear());
        binding.btnPlayPause.setOnClickListener(v -> OnClickBtnPlayPause());
        binding.btnNext.setOnClickListener(v -> OnClickBtnNext());
        binding.btnPrevious.setOnClickListener(v -> OnClickBtnPrevious());
        binding.btnStop.setOnClickListener(v -> OnClickBtnStop());
        binding.seekBar.setOnSeekBarChangeListener(CreateSeekBarListener());
   }

    private void connectToService() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();

                // Add Listener to handle icon changes automatically
                controller.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        updatePlayPauseIcon(isPlaying);
                    }
                });

                // Set initial icon state
                updatePlayPauseIcon(controller.isPlaying());

                progressHandler.post(progressRunnable);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void updatePlayPauseIcon(boolean isPlaying) {
        if (isPlaying) {
            binding.btnPlayPause.setIconResource(android.R.drawable.ic_media_pause);
        } else {
            binding.btnPlayPause.setIconResource(android.R.drawable.ic_media_play);
        }
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
                Toast.makeText(this,"Notification permission denied. Playback notification may not appear.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        progressHandler.removeCallbacks(progressRunnable);

        if (controller != null) {
            controller.release();
            controller = null;
        }
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (controller != null && controller.isConnected()) {
                long position = controller.getCurrentPosition();
                long duration = controller.getDuration();

                if (duration > 0) {
                    int progress = (int) ((position * 1000) / duration);
                    if (isUserInteractingWithSeekingBar == false) { // Only update if the user IS NOT touching the bar
                        binding.seekBar.setProgress(progress);
                    }
                    binding.txtTime.setText( MyUtils.formatTime(position) + " / " + MyUtils.formatTime(duration) );
                }

                MediaItem item = controller.getCurrentMediaItem();

                if (item != null && item.mediaMetadata != null && item.mediaMetadata.title != null) {
                    binding.txtTrackTitle.setText(item.mediaMetadata.title.toString());
                }
            }

            progressHandler.postDelayed(this, 500);
        }
    };

    //region UI Listeners

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        openPickerLauncher.launch(intent);
    }

    private void OnClickBtnClear() {
        TrackRepository.clearTracks(this);
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("LOAD_PLAYLIST");
        ContextCompat.startForegroundService(this, intent);
    }

    private void OnClickBtnPlayPause() {
        if (controller != null) {
            if (controller.isPlaying()) {
                controller.pause();
            } else {
                controller.play();
            }
        }
    }

    private void OnClickBtnNext() {
        if (controller != null) {
            controller.seekToNextMediaItem();
        }
    }

    private void OnClickBtnPrevious(){
        if (controller != null) {
            controller.seekToPreviousMediaItem();
        }
    }

    private void OnClickBtnStop() {
        if (controller != null) {
            controller.stop();
            controller.seekTo(0);
            updatePlayPauseIcon(false); // After stop, ensure icon is set to Play
        }
    }

    private SeekBar.OnSeekBarChangeListener CreateSeekBarListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && controller != null) {
                    long duration = controller.getDuration();
                    long newPosition = (duration * progress) / 1000;
                    controller.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserInteractingWithSeekingBar = true; // User started dragging
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserInteractingWithSeekingBar = false; // User stopped dragging
            }
        };
    }

    //endregion
}