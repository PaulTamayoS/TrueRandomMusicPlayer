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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

    private String[] playlists = {"My Library", "Gym Mix", "Work Focus"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();

        connectToService();

        // Set up the listener for the Playlist Editor button
        binding.btnPlaylistEditor.setOnClickListener(v -> OnClickBtnPlaylistEditor());

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

        binding.volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && controller != null) {
                    // Convert 0-100 to 0.0-1.0
                    float volume = progress / 100f;
                    controller.setVolume(volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.volumeSeekBar.setProgress(30);

        ReloadDropDownSpinnerPlaylists();

//        ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                this,
//                android.R.layout.simple_spinner_item,
//                playlists
//        );
//
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        binding.spinnerPlaylists.setAdapter(adapter);

        binding.spinnerPlaylists.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = playlists[position];

                Toast.makeText(MainActivity.this, "Changing Playlist to : " + selected, Toast.LENGTH_SHORT).show();
                // TODO : Logic to tell MusicService to switch JSON keys


//                // Trigger the service to load this specific JSON
//                Intent intent = new Intent(MainActivity.this, MusicService.class);
//                intent.setAction("LOAD_PLAYLIST");
//                intent.putExtra("PLAYLIST_NAME", selected);
//                startService(intent);


                //binding.seekBar.setProgress(0);
                //binding.txtTime.setText( R.string.player_time_zero);
                //binding.txtTrackTitle.setText(R.string.no_track_playing);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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
    protected void onResume() {
        super.onResume();
        // This runs every time you come back from the Editor
        OnResumeRefreshPlaylistSpinner();
    }

    private void OnResumeRefreshPlaylistSpinner() {
        // TODO: Later, load this array from SharedPreferences/TrackRepository
        // playlists = TrackRepository.getPlaylistNames(this);


        ReloadDropDownSpinnerPlaylists();

        // Optional: Auto-select the last active playlist
    }

    private void ReloadDropDownSpinnerPlaylists()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                playlists
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPlaylists.setAdapter(adapter);
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

    private void OnClickBtnPlaylistEditor()
    {
        // Create an Intent to transition from this activity to the Manage Activity
        Intent intent = new Intent(MainActivity.this, ManagePlaylistsActivity.class);

        // Optional: Pass the name of the currently selected playlist
        // so the editor knows which one to load
        String selectedPlaylist = binding.spinnerPlaylists.getSelectedItem().toString();
        intent.putExtra("playlist_name", selectedPlaylist);

        startActivity(intent);
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

    //region UI Skin Swapper

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

            // Make the SeekBar orange too!
            binding.seekBar.getProgressDrawable().setTint(madnessColor);
            binding.seekBar.getThumb().setTint(madnessColor);

            binding.volumeSeekBar.getProgressDrawable().setTint(madnessColor);
            binding.volumeSeekBar.getThumb().setTint(madnessColor);
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

            // Reset SeekBar to theme primary
            binding.seekBar.getProgressDrawable().setTint(themePrimaryColor);
            binding.seekBar.getThumb().setTint(themePrimaryColor);

            binding.volumeSeekBar.getProgressDrawable().setTint(themePrimaryColor);
            binding.volumeSeekBar.getThumb().setTint(themePrimaryColor);
        }
    }

    //endregion
}