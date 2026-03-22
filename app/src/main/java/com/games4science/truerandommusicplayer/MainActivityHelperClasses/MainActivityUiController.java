package com.games4science.truerandommusicplayer.MainActivityHelperClasses;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import com.games4science.truerandommusicplayer.databinding.ActivityMainBinding;
import com.games4science.truerandommusicplayer.R;

public class MainActivityUiController {

    private final ActivityMainBinding binding;
    private final Context context;

    public MainActivityUiController(Context context, ActivityMainBinding binding) {
        this.context = context;
        this.binding = binding;
    }

    public void updatePlayPauseIcon(boolean isPlaying) {
        if (isPlaying) {
            binding.btnPlayPause.setIconResource(R.drawable.ic_pause);
        } else {
            binding.btnPlayPause.setIconResource(R.drawable.ic_play);
        }
    }

    public void applyMadnessTheme(boolean isMadness) {
        // 1. Define the Madness color (Orange)
        int madnessColor = ContextCompat.getColor(context, android.R.color.holo_orange_dark);

        // 2. Fetch the "Normal" Primary color from the current theme (Day or Night)
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
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
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true);
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

    public String GetSelectedPlayListName()
    {
        return binding.spinnerPlaylists.getSelectedItem().toString();
    }

}
