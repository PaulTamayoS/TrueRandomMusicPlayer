package com.games4science.truerandommusicplayer.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.games4science.truerandommusicplayer.data.TrackRepository;
import com.games4science.truerandommusicplayer.databinding.ActivityManagePlaylistsBinding;
import com.games4science.truerandommusicplayer.player.MusicService;
import com.games4science.truerandommusicplayer.ui.adapters.TrackAdapter;
import com.games4science.truerandommusicplayer.util.MyConstants;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ManagePlaylistsActivity extends AppCompatActivity {

    private ActivityManagePlaylistsBinding binding;
    private String currentPlaylistName = ""; // Used if we are editing an existing list

    private TrackAdapter trackAdapter;
    private List<JSONObject> trackList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagePlaylistsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if we are EDITING an existing playlist
        currentPlaylistName = getIntent().getStringExtra("playlist_name");

        if (currentPlaylistName == null || currentPlaylistName.isEmpty()) {
            currentPlaylistName = MyConstants.DEFAULT_PLAYLIST_NAME;
        }

        binding.rvTrackList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        trackAdapter = new TrackAdapter(trackList, (uri, position) -> confirmTrackRemoval(uri, position));
        binding.rvTrackList.setAdapter(trackAdapter);

        loadTracksIntoList();

        binding.editTextPlaylistName.setText(currentPlaylistName);
        updateSongCountUI();

        setupButtons();
    }

    private void updateSongCountUI() {
        String currentName = binding.editTextPlaylistName.getText().toString().trim();
        int count = TrackRepository.getTracksCount(this, currentName);
        binding.tvTotalSongsInPlaylist.setText(count + " Songs in this Playlist");
    }

    private void setupButtons() {
        binding.btnNewPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        binding.btnAddMusic.setOnClickListener(v -> openPicker(v));
        binding.btnClearLibrary.setOnClickListener(v ->  OnClickBtnClearLibrary());
        binding.btnSave.setOnClickListener(v -> handleDoneAndExit());
    }

    private void loadTracksIntoList() {
        trackList.clear();
        // Use the new getTrackObjects method we discussed for TrackRepository
        trackList.addAll(TrackRepository.getTrackObjects(this, currentPlaylistName));

        if (trackAdapter != null) {
            trackAdapter.notifyDataSetChanged();
        }
    }

    private void confirmTrackRemoval(String uri, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Track")
                .setMessage("Remove this song from the playlist?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // 1. Remove from Repository
                    TrackRepository.removeSingleTrack(this, currentPlaylistName, uri);

                    // 2. Update local list and UI
                    trackList.remove(position);
                    trackAdapter.notifyItemRemoved(position);
                    updateSongCountUI();

                    // 3. Sync Service
                    MainActivity.playlistModified = true;
                    LoadOrReloadMusicService();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void LoadOrReloadMusicService()
    {
        // 1. Get the name of the playlist being edited
        String nameToLoad = binding.editTextPlaylistName.getText().toString().trim();

        // Fallback if empty
        if (nameToLoad.isEmpty()) {
            nameToLoad = currentPlaylistName;
        }

        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MyConstants.LOAD_PLAYLIST);
        serviceIntent.putExtra("PLAYLIST_NAME", nameToLoad);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    //region UI Listeners

    private void OnClickBtnClearLibrary() {
        String currentName = binding.editTextPlaylistName.getText().toString().trim();

        // Create the confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Clear Playlist")
                .setMessage("Are you sure you want to remove all tracks from '" + currentName + "'? This cannot be undone, and will delete the playlist if no tracks are added.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    // This code only runs if the user clicks "Clear All"
                    executeClearLibrary(currentName);
                })
                .setNegativeButton("Cancel", null) // Does nothing and closes dialog
                .show();
    }

    private void executeClearLibrary(String nameToClear) {
        TrackRepository.clearTracks(this, nameToClear);

        // We need to reload the music service if the playlist currently playing is modified
        if (currentPlaylistName != null && currentPlaylistName.isEmpty() == false) {
            if (currentPlaylistName.equals(nameToClear)) {
                LoadOrReloadMusicService();
            }
        }

        trackList.clear();
        if (trackAdapter != null) {
            trackAdapter.notifyDataSetChanged();
        }

        updateSongCountUI(); // Use your helper to refresh the text
        Toast.makeText(this, "Playlist cleared", Toast.LENGTH_SHORT).show();

        MainActivity.playlistModified = true;
    }

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

    private void handleDoneAndExit() {
        String newName = binding.editTextPlaylistName.getText().toString().trim();

        if (newName.isEmpty()) {
            binding.editTextPlaylistName.setError("Name is required");
            return;
        }

        // If the user changed the name in the EditText, perform a rename in storage
        if (newName.equals(currentPlaylistName) == false) {
            TrackRepository.renamePlaylist(this, currentPlaylistName, newName);
            currentPlaylistName = newName;
            MainActivity.playlistModified = true; // Tell MainActivity that something changed!
        }

        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(this);
        input.setHint("e.g., New Mix");

        new AlertDialog.Builder(this)
                .setTitle("New Playlist")
                .setMessage("Enter a name for your new playlist:")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createNewPlaylist(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //endregion


    //region UI Listeners Sub Methods

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

                                String nameToSaveTo = binding.editTextPlaylistName.getText().toString().trim();
                                if (nameToSaveTo.isEmpty()) {
                                    nameToSaveTo = currentPlaylistName;
                                }

                                // 1. Single file selection
                                if (data.getData() != null) {
                                    TrackRepository.saveTrack(this, nameToSaveTo, data.getData());
                                    countAddedTracks++;
                                }
                                // 2. Multiple file selection
                                else if (data.getClipData() != null) {
                                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                        Uri uri = data.getClipData().getItemAt(i).getUri();
                                        TrackRepository.saveTrack(this, nameToSaveTo, uri);
                                        countAddedTracks++;
                                    }
                                }

                                // 3. Update UI and Service once finished
                                if (countAddedTracks > 0) {
                                    int finalCount = countAddedTracks;
                                    MainActivity.playlistModified = true;
                                    currentPlaylistName = nameToSaveTo;
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Added " + finalCount + " tracks!", Toast.LENGTH_SHORT).show();
                                        loadTracksIntoList();
                                        LoadOrReloadMusicService();
                                        updateSongCountUI();
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

                    binding.tvTotalSongsInPlaylist.setText("Deep scanning folders... please WAIT!!!!!");

                    // Run this in a background thread so the UI doesn't freeze
                    new Thread(() -> {
                        try {
                            String nameToSaveTo = binding.editTextPlaylistName.getText().toString().trim();
                            if (nameToSaveTo.isEmpty()) {
                                nameToSaveTo = currentPlaylistName;
                            }

                            // The heavy lifting happens here
                            int countAddedTracks = TrackRepository.saveTracksFromFolder(this, nameToSaveTo, folderUri);
                            MainActivity.playlistModified = true;
                            currentPlaylistName = nameToSaveTo;

                            runOnUiThread(() -> {
                                loadTracksIntoList();
                                LoadOrReloadMusicService();
                                updateSongCountUI();
                                Toast.makeText(this, "Scan complete! Total tracks added = " + countAddedTracks, Toast.LENGTH_SHORT).show();
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

    private void createNewPlaylist(String name) {
        java.util.List<String> existingNames = TrackRepository.getAllPlaylistNames(this);

        if (existingNames.contains(name)) {
            Toast.makeText(this, "Playlist '" + name + "' already exists!", Toast.LENGTH_SHORT).show();
            this.currentPlaylistName = name;
            binding.editTextPlaylistName.setText(name);
            updateSongCountUI();
            return;
        }

        TrackRepository.initializeEmptyPlaylist(this, name);

        this.currentPlaylistName = name;
        binding.editTextPlaylistName.setText(name);
        MainActivity.playlistModified = true;

        updateSongCountUI(); // Refresh the UI to show 0 tracks

        Toast.makeText(this, "Playlist '" + name + "' created!", Toast.LENGTH_SHORT).show();
    }

    //endregion
}