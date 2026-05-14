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

import com.games4science.truerandommusicplayer.api.RetrofitClient;
import com.games4science.truerandommusicplayer.api.SubsonicApi;
import com.games4science.truerandommusicplayer.api.SubsonicResponse;
import com.games4science.truerandommusicplayer.data.TrackRepository;
import com.games4science.truerandommusicplayer.databinding.ActivityManagePlaylistsBinding;
import com.games4science.truerandommusicplayer.model.Playlist;
import com.games4science.truerandommusicplayer.model.Track;
import com.games4science.truerandommusicplayer.player.MusicService;
import com.games4science.truerandommusicplayer.ui.adapters.TrackAdapter;
import com.games4science.truerandommusicplayer.util.MyConstants;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ManagePlaylistsActivity extends AppCompatActivity {

    private ActivityManagePlaylistsBinding binding;
    private String initialPlaylistName = ""; // Used if we are editing an existing list
    private long currentPlaylistId = -1;

    private TrackAdapter trackAdapter;
    private final List<Track> trackList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagePlaylistsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if we are EDITING an existing playlist
        currentPlaylistId = getIntent().getLongExtra(MyConstants.EXTRA_PLAYLIST_ID_TO_EDIT, -1);
        initialPlaylistName = getIntent().getStringExtra(MyConstants.EXTRA_PLAYLIST_NAME_TO_EDIT);

        binding.rvTrackList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        trackAdapter = new TrackAdapter(trackList, (uri, position) -> confirmTrackRemoval(uri, position));
        binding.rvTrackList.setAdapter(trackAdapter);

        loadTracksIntoList();

        binding.editTextPlaylistName.setText(initialPlaylistName);

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
        TrackRepository.getTracksAsModels(this, currentPlaylistId, tracks -> {
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

    private void LoadOrReloadMusicService()
    {
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MyConstants.ACTION_LOAD_PLAYLIST);
        serviceIntent.putExtra(MyConstants.EXTRA_PLAYLIST_NAME, initialPlaylistName);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    //region UI Listeners

    private void openPicker(View v) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, v);
        popup.getMenu().add("Select Files");
        popup.getMenu().add("Select Folder");
        popup.getMenu().add("Import From Server");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Select Files")) {
                openFilePicker();
            } else if (item.getTitle().equals("Select Folder")) {
                openFolderPicker();
            }
            else {
                showImportSubsonicDialogGetAllPlaylists();
            }
            return true;
        });
        popup.show();
    }

    private void OnClickBtnDeleteLibrary() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete the playlist? This cannot be undone.")
                .setPositiveButton("Delete Playlist", (dialog, which) -> {
                    executeDeletePlaylist(currentPlaylistId);
                })
                .setNegativeButton("Cancel", null) // Does nothing and closes dialog
                .show();
    }

    private void handleDoneAndExit() {
        String newName = binding.editTextPlaylistName.getText().toString().trim();

        if (newName.isEmpty()) {
            binding.editTextPlaylistName.setError("Name is required");
            return;
        }

        if (newName.equals(initialPlaylistName)) {
            finish(); // No changes made
            return;
        }

        // If the user changed the name in the EditText, perform a rename
        TrackRepository.renamePlaylistById(this, currentPlaylistId, newName, success -> {
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

    private void confirmTrackRemoval(String uri, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Track")
                .setMessage("Remove this song from the playlist?")
                .setPositiveButton("Remove", (dialog, which) -> {

                    TrackRepository.removeSingleTrack(this, currentPlaylistId, uri);

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

                            TrackRepository.saveTracksFromFilesPicker(this, currentPlaylistId, urisToProcess, countAdded -> {
                                runOnUiThread(() -> {
                                    if (countAdded > 0) {
                                        MainActivity.playlistModified = true;
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
                            // The heavy lifting happens here
                            TrackRepository.saveTracksFromFolder(this, currentPlaylistId, folderUri, addedTracks -> {
                                runOnUiThread(() -> {
                                    MainActivity.playlistModified = true;
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
        TrackRepository.getAllPlaylists(this, existingPlaylists -> {
            runOnUiThread(() -> {

                for (Playlist p : existingPlaylists) {
                    if (p.playlistName.equals(name)) {
                        Toast.makeText(this, "Playlist '" + name + "' already exists!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                TrackRepository.createPlaylist(this, name, id -> {
                    runOnUiThread(() -> {
                        currentPlaylistId = id;
                        this.initialPlaylistName = name;
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

    private void executeDeletePlaylist(long playlistId) {
        TrackRepository.deletePlaylistById(this, playlistId, deletedName -> {
            runOnUiThread(() -> {
                MainActivity.playlistModified = true; // Tell MainActivity that something changed!
                LoadOrReloadMusicService();
                finish(); // Return to MainActivity after deleting the playlist
            });
        });
    }

    private void showImportSubsonicDialogGetAllPlaylists() {
        SubsonicApi api = RetrofitClient.getSubsonicApi(this);
        if (api == null) {
            Toast.makeText(this, "Configure server settings first!", Toast.LENGTH_SHORT).show();
            return;
        }

        RetrofitClient.executeRequest(this, api.getPlaylists(), data -> {
            if (data.getPlaylists() != null && data.getPlaylists().getPlaylist() != null)
            {
                List<SubsonicResponse.Playlist> remotePlaylists = data.getPlaylists().getPlaylist();
                Toast.makeText(this, "Success! Found " + remotePlaylists.size() + " playlists.", Toast.LENGTH_SHORT).show();
               // showPlaylistSelectionDialog(playlists);
            }
            else
            {
                Toast.makeText(this, "Connected, but you have 0 playlists on the server.", Toast.LENGTH_LONG).show();
            }
        });
    }



    //endregion
}