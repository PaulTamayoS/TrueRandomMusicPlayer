package com.games4science.truerandommusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.games4science.truerandommusicplayer.MainActivityHelperClasses.MainActivityActionHandler;
import com.games4science.truerandommusicplayer.MainActivityHelperClasses.MainActivityUiController;
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

    private MainActivityUiController uiController;
    private MainActivityActionHandler actionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize the UI Controller
        uiController = new MainActivityUiController(this, binding);

        requestNotificationPermission();

        connectToService();

        // Set up the listener for the Playlist Editor button
        binding.btnPlaylistEditor.setOnClickListener(v -> actionHandler.OnClickBtnPlaylistEditor());
        binding.btnPlayPause.setOnClickListener(v -> actionHandler.OnClickBtnPlayPause());
        binding.btnNext.setOnClickListener(v -> actionHandler.OnClickBtnNext());
        binding.btnPrevious.setOnClickListener(v -> actionHandler.OnClickBtnPrevious());
        binding.btnStop.setOnClickListener(v -> actionHandler.OnClickBtnStop());
        binding.seekBar.setOnSeekBarChangeListener(CreateSeekBarListener());
        binding.switchPureRandom.setOnCheckedChangeListener((buttonView, isChecked) -> actionHandler.OnTogglePureRandom(isChecked));

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

                // Now that we have a controller, initialize the Action Handler
                actionHandler = new MainActivityActionHandler(this, controller, uiController);

                // Add Listener to handle icon changes automatically
                controller.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        uiController.updatePlayPauseIcon(isPlaying);
                    }
                });

                // Set initial icon state
                uiController.updatePlayPauseIcon(controller.isPlaying());

                progressHandler.post(progressRunnable);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
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