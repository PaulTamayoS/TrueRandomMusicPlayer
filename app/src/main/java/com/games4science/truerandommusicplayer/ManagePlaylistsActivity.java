package com.games4science.truerandommusicplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.games4science.truerandommusicplayer.data.TrackRepository;
import com.games4science.truerandommusicplayer.databinding.ActivityManagePlaylistsBinding;
import com.games4science.truerandommusicplayer.player.MusicService;

import org.json.JSONArray;

public class ManagePlaylistsActivity extends AppCompatActivity {

    private ActivityManagePlaylistsBinding binding;
    private JSONArray currentPlaylistSongs = new JSONArray();
    private String currentPlaylistName = ""; // Used if we are editing an existing list

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagePlaylistsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if we are EDITING an existing playlist
        currentPlaylistName = getIntent().getStringExtra("playlist_name");

        if (currentPlaylistName == null || currentPlaylistName.isEmpty()) {
            currentPlaylistName = "My Library";
        }
        else {
            //loadExistingPlaylistData(originalName);
        }

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
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveAndExit());
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
        serviceIntent.setAction("LOAD_PLAYLIST");
        serviceIntent.putExtra("PLAYLIST_NAME", nameToLoad);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    //region UI Listeners

    private void OnClickBtnClearLibrary() {

        //TODO Popup to confirm that deleting cant be unmade.

        String currentName = binding.editTextPlaylistName.getText().toString().trim();
        TrackRepository.clearTracks(this, currentName);

        //We need to reload the music service if the playlist currently playing is modified
        if (currentPlaylistName != null && currentPlaylistName.isEmpty() == false)
        {
            if (currentPlaylistName.equals(currentName))
            {
                LoadOrReloadMusicService();
            }
        }

        binding.tvTotalSongsInPlaylist.setText("0 Songs in this Playlist");
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

    private void saveAndExit() {
        String newName = binding.editTextPlaylistName.getText().toString().trim();

        if (newName.isEmpty()) {
            binding.editTextPlaylistName.setError("Name is required");
            return;
        }

        // Logic to save to TrackRepository
        // TrackRepository.saveFullPlaylist(this, newName, currentPlaylistSongs);

        // If the user typed a new name in the box, we should treat that as the playlist name
        this.currentPlaylistName = newName;

        // Tell MainActivity that something changed!
        MainActivity.playlistModified = true;

        Toast.makeText(this, "Playlist Saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(this);
        input.setHint("e.g., Gym Mix");

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

                                // 1. Single file selection
                                if (data.getData() != null) {
                                    TrackRepository.saveTrack(this, currentPlaylistName, data.getData());
                                    countAddedTracks++;
                                }
                                // 2. Multiple file selection
                                else if (data.getClipData() != null) {
                                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                        Uri uri = data.getClipData().getItemAt(i).getUri();
                                        TrackRepository.saveTrack(this, currentPlaylistName, uri);
                                        countAddedTracks++;
                                    }
                                }

                                // 3. Update UI and Service once finished
                                if (countAddedTracks > 0) {
                                    int finalCount = countAddedTracks;
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Added " + finalCount + " tracks!", Toast.LENGTH_SHORT).show();
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
                            // The heavy lifting happens here
                            int countAddedTracks = TrackRepository.saveTracksFromFolder(this, currentPlaylistName, folderUri);

                            runOnUiThread(() -> {
                                LoadOrReloadMusicService();
                                updateSongCountUI();
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

    private void createNewPlaylist(String name) {

        java.util.List<String> existingNames = TrackRepository.getAllPlaylistNames(this);

        if (existingNames.contains(name)) {
            Toast.makeText(this, "Playlist '" + name + "' already exists!", Toast.LENGTH_SHORT).show();
            // Just switch to it instead of clearing it
            this.currentPlaylistName = name;
            binding.editTextPlaylistName.setText(name);
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