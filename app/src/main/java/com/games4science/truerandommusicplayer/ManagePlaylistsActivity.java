package com.games4science.truerandommusicplayer;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.games4science.truerandommusicplayer.databinding.ActivityManagePlaylistsBinding;

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

        // 2. Clear List
        binding.btnClearLibrary.setOnClickListener(v -> {
            currentPlaylistSongs = new JSONArray();
            updateSongCountUI();
            Toast.makeText(this, "Playlist cleared", Toast.LENGTH_SHORT).show();
        });

        // 3. Cancel
        binding.btnCancel.setOnClickListener(v -> finish());

        // 4. Save
        binding.btnSave.setOnClickListener(v -> saveAndExit());
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

    private void updateSongCountUI() {
        binding.tvTotalSongsInPlaylist.setText(currentPlaylistSongs.length() + " Songs in this Playlist");
    }


}