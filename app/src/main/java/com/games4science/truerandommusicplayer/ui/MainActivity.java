package com.games4science.truerandommusicplayer.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.games4science.truerandommusicplayer.MainActivityHelperClasses.MainActivityActionHandler;
import com.games4science.truerandommusicplayer.MainActivityHelperClasses.MainActivityUiController;
import com.games4science.truerandommusicplayer.R;
import com.games4science.truerandommusicplayer.databinding.ActivityMainBinding;
import com.games4science.truerandommusicplayer.model.Playlist;
import com.games4science.truerandommusicplayer.player.MusicService;
import com.games4science.truerandommusicplayer.data.TrackRepository;
import com.games4science.truerandommusicplayer.util.MyConstants;
import com.games4science.truerandommusicplayer.util.MyUtils;
import com.google.common.util.concurrent.ListenableFuture;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final int REQUEST_NOTIF = 100;

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;

    private Handler progressHandler = new Handler(Looper.getMainLooper());

    private String[] playlists = {MyConstants.DEFAULT_PLAYLIST_NAME};
    public List<Playlist> playlistObjects = new ArrayList<>();
    public static boolean playlistModified = false;
    private boolean isSpinnerTouched = false;
    private boolean isControllerSuccessfullyConnected = false;

    private MainActivityUiController uiController;
    private MainActivityActionHandler actionHandler;

    @SuppressLint("ClickableViewAccessibility")
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
        binding.btnServerSettings.setOnClickListener(v -> actionHandler.OnClickBtnServerSettings());
        binding.btnPlayPause.setOnClickListener(v -> actionHandler.OnClickBtnPlayPause());
        binding.btnNext.setOnClickListener(v -> actionHandler.OnClickBtnNext());
        binding.btnPrevious.setOnClickListener(v -> actionHandler.OnClickBtnPrevious());
        binding.btnStop.setOnClickListener(v -> actionHandler.OnClickBtnStop());
        binding.switchPureRandom.setOnCheckedChangeListener((buttonView, isChecked) -> actionHandler.OnTogglePureRandom(isChecked));
        OnResumeRefreshPlaylistSpinner();
        binding.spinnerPlaylists.setOnItemSelectedListener(CreateSpinnerPlaylistsItemSelectedListener());

        binding.spinnerPlaylists.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                isSpinnerTouched = true;
            }
            return false; // Return false so the spinner still opens normally
        });
    }

    private void connectToService() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                isControllerSuccessfullyConnected = true; // Flag the connection is ready

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

                // If the service has no tracks, it means we just booted and need to load the first playlist
                if (controller.getMediaItemCount() == 0) {
                    String initialPlaylist = binding.spinnerPlaylists.getSelectedItem().toString();
                    triggerPlaylistLoad(initialPlaylist);
                }

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
        TrackRepository.getAllPlaylists(this, existingPlaylists -> {
            runOnUiThread(() -> {
                playlistObjects = existingPlaylists;
                playlists = new String[existingPlaylists.size()];

                for (int i = 0; i < existingPlaylists.size(); i++) {
                    playlists[i] = existingPlaylists.get(i).playlistName;
                }

                uiController.ReloadDropDownSpinnerPlaylists(playlists);

                // Get the last known playlist name
                String lastSaved = getSharedPreferences(MyConstants.PREFS_REPO_PLAYER, MODE_PRIVATE).getString(MyConstants.PREFS_KEY_LAST_PLAYLIST, MyConstants.DEFAULT_PLAYLIST_NAME);

                // Update the Spinner selection to match
                for (int i = 0; i < playlists.length; i++) {
                    if (playlists[i].equals(lastSaved)) {
                        binding.spinnerPlaylists.setSelection(i);
                        break;
                    }
                }
            });
        });
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
                    if (actionHandler!= null && actionHandler.IsUserInteractingWithTrackSeekingBar() == false) { // Only update if the user IS NOT touching the bar
                        binding.trackSeekBar.setProgress(progress);
                    }
                    binding.txtTime.setText(MyUtils.formatTime(position) + " / " + MyUtils.formatTime(duration) );
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
                else if (controller.getMediaItemCount() == 0) {
                    binding.txtTrackTitle.setText("Playlist is empty");
                } else {
                    binding.txtTrackTitle.setText("Preparing...");// Service is connected but still preparing the first track
                }
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    public AdapterView.OnItemSelectedListener CreateSpinnerPlaylistsItemSelectedListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // If we aren't connected yet, just sync the variable and wait. This prevents the "Auto-Selection" on boot from triggering a load.
                if (!isControllerSuccessfullyConnected) {
                    return;
                }

                if (isSpinnerTouched == false){
                    return;
                }

                isSpinnerTouched = false;
                triggerPlaylistLoad(playlists[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    private void triggerPlaylistLoad(String playlistName) {
        // Save to disk so we remember even if the app process dies
        getSharedPreferences(MyConstants.PREFS_REPO_PLAYER, MODE_PRIVATE)
                .edit()
                .putString(MyConstants.PREFS_KEY_LAST_PLAYLIST, playlistName)
                .apply();

        // Tell the MusicService to switch JSON keys and reload
        android.content.Intent intent = new android.content.Intent(MainActivity.this, MusicService.class);
        intent.setAction(MyConstants.ACTION_LOAD_PLAYLIST);
        intent.putExtra(MyConstants.EXTRA_PLAYLIST_NAME, playlistName);
        startService(intent);

        //Reset UI display while loading
        binding.trackSeekBar.setProgress(0);
        binding.txtTime.setText( R.string.player_time_zero);
        binding.txtTrackTitle.setText("Loading " + playlistName + "...");
    }
}