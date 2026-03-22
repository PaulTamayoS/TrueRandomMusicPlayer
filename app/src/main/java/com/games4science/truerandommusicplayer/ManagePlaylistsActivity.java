package com.games4science.truerandommusicplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private String originalName = ""; // Used if we are editing an existing list

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagePlaylistsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if we are EDITING an existing playlist
        originalName = getIntent().getStringExtra("playlist_name");
        if (originalName != null || originalName.isEmpty() == false) {
            binding.editTextPlaylistName.setText(originalName);
            //loadExistingPlaylistData(originalName);
        }

        setupButtons();
    }

    private void setupButtons() {
        // 1. Scan Folders
        binding.btnAddMusic.setOnClickListener(v -> {
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//            folderPickerLauncher.launch(intent);
        });

        binding.btnAddMusic.setOnClickListener(v -> openPicker(v));
        binding.btnClearLibrary.setOnClickListener(v ->  OnClickBtnClearLibrary());
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveAndExit());
    }

    //region UI Listeners

    private void OnClickBtnClearLibrary() {
        TrackRepository.clearTracks(this);
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("LOAD_PLAYLIST");
        ContextCompat.startForegroundService(this, intent);

        binding.tvTotalSongsInPlaylist.setText(currentPlaylistSongs.length() + " Songs in this Playlist");
        Toast.makeText(this, "Playlist cleared", Toast.LENGTH_SHORT).show();

        //binding.seekBar.setProgress(0);
        //binding.txtTime.setText( R.string.player_time_zero);
        //binding.txtTrackTitle.setText(R.string.no_track_playing);
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

        Toast.makeText(this, "Playlist Saved!", Toast.LENGTH_SHORT).show();
        finish();
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

                    binding.tvTotalSongsInPlaylist.setText("Deep scanning folders... please WAIT!!!!!");

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

    //endregion
}