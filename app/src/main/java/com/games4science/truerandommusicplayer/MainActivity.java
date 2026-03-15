package com.games4science.truerandommusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
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

        binding.btnAddMusic.setOnClickListener(v -> openPicker(v));
        binding.btnClearLibrary.setOnClickListener(v ->  OnClickBtnClearLibrary());
        binding.btnPlayPause.setOnClickListener(v -> OnClickBtnPlayPause());
        binding.btnNext.setOnClickListener(v -> OnClickBtnNext());
        binding.btnPrevious.setOnClickListener(v -> OnClickBtnPrevious());
        binding.btnStop.setOnClickListener(v -> OnClickBtnStop());
        binding.seekBar.setOnSeekBarChangeListener(CreateSeekBarListener());

        binding.switchPureRandom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(this, MusicService.class);
            intent.setAction("TOGGLE_PURE_RANDOM");
            intent.putExtra("STATE", isChecked);
            startService(intent);

            applyMadnessTheme(isChecked); // Triggers the "Madness" UI change
        });
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
            binding.btnPlayPause.setIconResource(R.drawable.ic_pause);
        } else {
            binding.btnPlayPause.setIconResource(R.drawable.ic_play);
        }
    }

    private final ActivityResultLauncher<Intent> openPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();

                            // Show a quick toast so the user knows something is happening
                            Toast.makeText(this, "Adding files...", Toast.LENGTH_SHORT).show();

                            // Move the loop to a background thread
                            new Thread(() -> {
                                int countAddedTracks = 0;

                                // 1. Single file selection
                                if (data.getData() != null) {
                                    TrackRepository.saveTrack(this, data.getData());
                                    countAddedTracks++;
                                }
                                // 2. Multiple file selection
                                else if (data.getClipData() != null) {
                                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                        Uri uri = data.getClipData().getItemAt(i).getUri();
                                        TrackRepository.saveTrack(this, uri);
                                        countAddedTracks++;
                                    }
                                }

                                // 3. Update UI and Service once finished
                                if (countAddedTracks > 0) {
                                    int finalCount = countAddedTracks;
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Added " + finalCount + " tracks!", Toast.LENGTH_SHORT).show();

                                        Intent serviceIntent = new Intent(this, MusicService.class);
                                        serviceIntent.setAction("LOAD_PLAYLIST");
                                        ContextCompat.startForegroundService(this, serviceIntent);
                                    });
                                }
                            }).start();
                        }
                    }
            );

    private final ActivityResultLauncher<Uri> openFolderLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), folderUri -> {
                if (folderUri != null) {
                    // 1. Take persistable permission so we don't lose access on reboot
                    getContentResolver().takePersistableUriPermission(folderUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    binding.txtTrackTitle.setText("Deep scanning folders... please WAIT!!!!!");

                    // Run this in a background thread so the UI doesn't freeze
                    new Thread(() -> {
                        try {
                            // The heavy lifting happens here
                            int countAddedTracks = TrackRepository.saveTracksFromFolder(this, folderUri);

                            runOnUiThread(() -> {
                                Intent serviceIntent = new Intent(this, MusicService.class);
                                serviceIntent.setAction("LOAD_PLAYLIST");
                                ContextCompat.startForegroundService(this, serviceIntent);

                                Toast.makeText(this, "Tracks added! Total = " + countAddedTracks, Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error scanning folder: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();
                }
            });

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

                if (item != null && item.mediaMetadata != null ) {
                    String title = item.mediaMetadata.title != null ? item.mediaMetadata.title.toString() : "Unknown Song";
                    String artist = item.mediaMetadata.artist != null ? item.mediaMetadata.artist.toString() : "X";

                    // Show "Current Index / Total Tracks" for better UX
                    int currentIdx = controller.getCurrentMediaItemIndex() + 1;
                    int total = controller.getMediaItemCount();
                    binding.txtTrackTitle.setText(artist +" - " + title + " (" + currentIdx + "/" + total + ")");
                }
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    //region UI Listeners

    private void openPicker(View v) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, v);
        popup.getMenu().add("Select Files");
        popup.getMenu().add("Select Folder");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Select Files")) {
                openFilePicker();
            } else {
                openFolderPicker();
            }
            return true;
        });
        popup.show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        openPickerLauncher.launch(intent);
    }

    private void openFolderPicker() {
        // This triggers the Android directory selector
        openFolderLauncher.launch(null);
    }

    private void OnClickBtnClearLibrary() {
        TrackRepository.clearTracks(this);
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("LOAD_PLAYLIST");
        ContextCompat.startForegroundService(this, intent);

        binding.seekBar.setProgress(0);
        binding.txtTime.setText( R.string.player_time_zero);
        binding.txtTrackTitle.setText(R.string.no_track_playing);
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

    private void applyMadnessTheme(boolean isMadness) {
        // 1. Define the Madness color (Orange)
        int madnessColor = ContextCompat.getColor(this, android.R.color.holo_orange_dark);

        // 2. Fetch the "Normal" Primary color from the current theme (Day or Night)
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        int themePrimaryColor = typedValue.data;

        // 3. Define the state lists
        ColorStateList madnessList = ColorStateList.valueOf(madnessColor);
        ColorStateList normalList = ColorStateList.valueOf(themePrimaryColor);

        if (isMadness) {
            // APPLY MADNESS
            binding.tvAppTitle.setText(R.string.app_title_madness_mode);
            binding.tvAppTitle.setTextColor(madnessColor);

            // Update all your buttons from the XML
            binding.btnPlayPause.setBackgroundTintList(madnessList);
            binding.btnNext.setBackgroundTintList(madnessList);
            binding.btnPrevious.setBackgroundTintList(madnessList);
            binding.btnStop.setBackgroundTintList(madnessList);
            binding.btnAddMusic.setBackgroundTintList(madnessList);

            // Make the SeekBar orange too!
            binding.seekBar.getProgressDrawable().setTint(madnessColor);
            binding.seekBar.getThumb().setTint(madnessColor);
        } else {
            // RESET TO NORMAL (Respects Day/Night)
            binding.tvAppTitle.setText(R.string.app_title_normal_mode);

            // For the title, we usually want the standard text color
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true);
            binding.tvAppTitle.setTextColor(typedValue.data);

            binding.btnPlayPause.setBackgroundTintList(normalList);
            binding.btnNext.setBackgroundTintList(normalList);
            binding.btnPrevious.setBackgroundTintList(normalList);
            binding.btnStop.setBackgroundTintList(normalList);
            binding.btnAddMusic.setBackgroundTintList(normalList);

            // Reset SeekBar to theme primary
            binding.seekBar.getProgressDrawable().setTint(themePrimaryColor);
            binding.seekBar.getThumb().setTint(themePrimaryColor);
        }
    }
}