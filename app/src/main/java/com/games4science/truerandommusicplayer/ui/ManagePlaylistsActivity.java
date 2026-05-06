package com.games4science.truerandommusicplayer.ui;

import android.annotation.SuppressLint;
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
import com.games4science.truerandommusicplayer.model.Track;
import com.games4science.truerandommusicplayer.player.MusicService;
import com.games4science.truerandommusicplayer.ui.adapters.TrackAdapter;
import com.games4science.truerandommusicplayer.util.MyConstants;

import java.util.ArrayList;
import java.util.List;

public class ManagePlaylistsActivity extends AppCompatActivity {

    private ActivityManagePlaylistsBinding binding;
    private String currentPlaylistName = ""; // Used if we are editing an existing list

    private TrackAdapter trackAdapter;
    private final List<Track> trackList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagePlaylistsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if we are EDITING an existing playlist
        currentPlaylistName = getIntent().getStringExtra(MyConstants.EXTRA_PLAYLIST_NAME_TO_EDIT);
        if (currentPlaylistName == null || currentPlaylistName.isEmpty()) {
            currentPlaylistName = MyConstants.DEFAULT_PLAYLIST_NAME;
        }

        binding.rvTrackList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        trackAdapter = new TrackAdapter(trackList, (uri, position) -> confirmTrackRemoval(uri, position));
        binding.rvTrackList.setAdapter(trackAdapter);

        loadTracksIntoList();

        binding.editTextPlaylistName.setText(currentPlaylistName); // TODO: Check if can reduce the number of times that I use currentPlaylistName and reading editTextPlaylistName

        setupButtons();
    }

    @SuppressLint("SetTextI18n")
    private void updateTracksCountUI() {
        binding.tvTotalSongsInPlaylist.setText(trackList.size() + " Songs in this Playlist");
    }

    private void setupButtons() {
        binding.btnNewPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        binding.btnAddMusic.setOnClickListener(v -> openPicker(v));
        binding.btnDeletePlaylist.setOnClickListener(v ->  OnClickBtnDeleteLibrary());
        binding.btnSave.setOnClickListener(v -> handleDoneAndExit());
    }

    private void loadTracksIntoList() {
        TrackRepository.getTracksAsModels(this, currentPlaylistName, tracks -> {
            runOnUiThread(() -> {
                trackList.clear();
                if (tracks != null) {
                    trackList.addAll(tracks);
                }
                if (trackAdapter != null) {
                    trackAdapter.notifyDataSetChanged();
                }
                updateTracksCountUI();
            });
        });
    }

    private void confirmTrackRemoval(String uri, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Track")
                .setMessage("Remove this song from the playlist?")
                .setPositiveButton("Remove", (dialog, which) -> {

                    TrackRepository.removeSingleTrack(this, currentPlaylistName, uri);

                    trackList.remove(position);
                    trackAdapter.notifyItemRemoved(position);
                    trackAdapter.notifyItemRangeChanged(position, trackList.size() - position);
                    updateTracksCountUI();

                    MainActivity.playlistModified = true;
                    LoadOrReloadMusicService();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void LoadOrReloadMusicService()
    {
        // Get the name of the playlist being edited
        String nameToLoad = binding.editTextPlaylistName.getText().toString().trim();

        // Fallback if empty
        if (nameToLoad.isEmpty()) {
            nameToLoad = currentPlaylistName;
        }

        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MyConstants.ACTION_LOAD_PLAYLIST);
        serviceIntent.putExtra(MyConstants.EXTRA_PLAYLIST_NAME, nameToLoad);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    //region UI Listeners

    private void OnClickBtnDeleteLibrary() {
        String currentName = binding.editTextPlaylistName.getText().toString().trim();

        // Create the confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete the playlist '" + currentName + "'? This cannot be undone.")
                .setPositiveButton("Delete Playlist", (dialog, which) -> {
                    executeDeletePlaylist(currentName);
                })
                .setNegativeButton("Cancel", null) // Does nothing and closes dialog
                .show();
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

        if (newName.isEmpty() && currentPlaylistName.equals("") == false) {
            binding.editTextPlaylistName.setError("Name is required");
            return;
        }

        if (newName.equals(currentPlaylistName)) {
            finish(); // No changes made
            return;
        }

        // If the user changed the name in the EditText, perform a rename
        TrackRepository.renamePlaylist(this, currentPlaylistName, newName, success -> {
            runOnUiThread(() -> {
                if (success) {
                    MainActivity.playlistModified = true; // Tell MainActivity that something changed!
                    finish();
                } else {
                    // Show error if the name was already taken
                    binding.editTextPlaylistName.setError("This playlist name already exists!");
                    Toast.makeText(this, "Could not rename: Name already taken", Toast.LENGTH_SHORT).show();
                }
            });
        });
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
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            List<Uri> urisToProcess = new ArrayList<>();

                            if (data.getData() != null) { // Single file selection
                                urisToProcess.add(data.getData());
                            } else if (data.getClipData() != null) { // Multiple file selection
                                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                    urisToProcess.add(data.getClipData().getItemAt(i).getUri());
                                }
                            }

                            if (urisToProcess.isEmpty()) return;

                            // Show a quick toast so the user knows something is happening
                            Toast.makeText(this, "Adding " + urisToProcess.size() + " files...", Toast.LENGTH_SHORT).show();

                            String nameToSaveTo = binding.editTextPlaylistName.getText().toString().trim();
                            if (nameToSaveTo.isEmpty()) {
                                nameToSaveTo = currentPlaylistName;
                            }
                            String localNameToSaveTo = nameToSaveTo;

                            TrackRepository.saveTracksFromFilesPicker(this, nameToSaveTo, urisToProcess, countAdded -> {
                                // ONLY update the UI once the background work is fully DONE
                                runOnUiThread(() -> {
                                    if (countAdded > 0) {
                                        MainActivity.playlistModified = true;
                                        currentPlaylistName = localNameToSaveTo;
                                        loadTracksIntoList();
                                        LoadOrReloadMusicService();
                                        Toast.makeText(this, "Added " + countAdded + " tracks!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
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

                            String playlistName = nameToSaveTo;

                            // The heavy lifting happens here
                            TrackRepository.saveTracksFromFolder(this, playlistName, folderUri, addedTracks -> {
                                runOnUiThread(() -> {
                                    MainActivity.playlistModified = true;
                                    currentPlaylistName = playlistName;

                                    loadTracksIntoList();
                                    LoadOrReloadMusicService();
                                    Toast.makeText(this, "Scan complete! Total tracks added = " + addedTracks.size(), Toast.LENGTH_SHORT).show();
                                });
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
        TrackRepository.getAllPlaylistNames(this, existingNames -> {
            runOnUiThread(() -> {
                if (existingNames.contains(name)) {
                    Toast.makeText(this, "Playlist '" + name + "' already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }

                TrackRepository.createPlaylist(this, name, id -> {
                    runOnUiThread(() -> {
                        this.currentPlaylistName = name;
                        binding.editTextPlaylistName.setText(name);
                        MainActivity.playlistModified = true;

                        trackList.clear();
                        if (trackAdapter != null) {
                            trackAdapter.notifyDataSetChanged();
                        }
                        updateTracksCountUI();
                        Toast.makeText(this, "Playlist '" + name + "' created!", Toast.LENGTH_SHORT).show();
                    });
                });
            });
        });
    }

    private void executeDeletePlaylist(String nameToClear) {
        TrackRepository.deletePlaylistByName(this, nameToClear, s -> {
            runOnUiThread(() -> {
                // We need to reload the music service if the playlist currently playing is modified
                if (currentPlaylistName != null && currentPlaylistName.isEmpty() == false) {
                    if (currentPlaylistName.equals(nameToClear)) {
                        binding.editTextPlaylistName.setText("");
                        currentPlaylistName = "";
                        LoadOrReloadMusicService();
                    }
                }

                trackList.clear();
                if (trackAdapter != null) {
                    trackAdapter.notifyDataSetChanged();
                }

                updateTracksCountUI();
                Toast.makeText(this, "Playlist deleted", Toast.LENGTH_SHORT).show();

                MainActivity.playlistModified = true;
            });
        });
    }

    //endregion
}