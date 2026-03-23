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


    private String[] playlists = {"My Library", "Gym Mix", "Work Focus"};
    public static boolean playlistModified = false;

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
        binding.switchPureRandom.setOnCheckedChangeListener((buttonView, isChecked) -> actionHandler.OnTogglePureRandom(isChecked));
        uiController.ReloadDropDownSpinnerPlaylists(playlists);
        binding.spinnerPlaylists.setOnItemSelectedListener(CreateSpinnerPlaylistsItemSelectedListener());
    }

    private void connectToService() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();

                // Now that we have a controller, initialize the Action Handler
                actionHandler = new MainActivityActionHandler(this, controller, uiController);

                binding.trackSeekBar.setOnSeekBarChangeListener(actionHandler.CreateTrackSeekBarListener());
                binding.volumeSeekBar.setOnSeekBarChangeListener(actionHandler.CreateVolumeSeekBarListener());
                binding.volumeSeekBar.setProgress(30);

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

        // Only reload if the flag was set to true by the other Activity
        if (playlistModified == true) {
            OnResumeRefreshPlaylistSpinner();
            playlistModified = false; // Reset the flag so we don't reload again next time
        }
    }

    private void OnResumeRefreshPlaylistSpinner() {
        // TODO: Later, load this array from SharedPreferences/TrackRepository
        // playlists = TrackRepository.getPlaylistNames(this);

        uiController.ReloadDropDownSpinnerPlaylists(playlists);

        // Optional: Auto-select the last active playlist
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
                    if (actionHandler!= null & actionHandler.IsUserInteractingWithTrackSeekingBar() == false) { // Only update if the user IS NOT touching the bar
                        binding.trackSeekBar.setProgress(progress);
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

    public AdapterView.OnItemSelectedListener CreateSpinnerPlaylistsItemSelectedListener() {
        return new AdapterView.OnItemSelectedListener() {
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
        };
    }
}