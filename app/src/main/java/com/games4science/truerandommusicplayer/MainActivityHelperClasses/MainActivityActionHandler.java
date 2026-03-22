package com.games4science.truerandommusicplayer.MainActivityHelperClasses;

import android.content.Intent;
import androidx.media3.session.MediaController;

import com.games4science.truerandommusicplayer.MainActivity;
import com.games4science.truerandommusicplayer.ManagePlaylistsActivity;
import com.games4science.truerandommusicplayer.player.MusicService;

public class MainActivityActionHandler {

    private final MainActivity activity;
    private final MediaController controller;
    private final MainActivityUiController uiController; //Normally, this should not be here. The MainActivity should give us the data that we need. But for a project this small it should be safe


    public MainActivityActionHandler(MainActivity activity, MediaController controller,MainActivityUiController uiController) {
        this.activity = activity;
        this.controller = controller;
        this.uiController = uiController;
    }

    public void OnClickBtnPlayPause() {
        if (controller != null) {
            if (controller.isPlaying()) {
                controller.pause();
            } else {
                controller.play();
            }
        }
    }

    public void OnClickBtnNext() {
        if (controller != null) {
            controller.seekToNextMediaItem();
        }
    }

    public void OnClickBtnPrevious() {
        if (controller != null) {
            controller.seekToPreviousMediaItem();
        }
    }

    public void OnClickBtnStop() {
        if (controller != null) {
            controller.stop();
            controller.seekTo(0);
            uiController.updatePlayPauseIcon(false); // After stop, ensure icon is set to Play
        }
    }

    public void OnClickBtnPlaylistEditor()
    {
        String selectedPlaylist = uiController.GetSelectedPlayListName();
        // Create an Intent to transition from this activity to the Manage Activity
        Intent intent = new Intent(activity, ManagePlaylistsActivity.class);
        intent.putExtra("playlist_name", selectedPlaylist);
        activity.startActivity(intent);
    }

    public void OnTogglePureRandom(boolean isChecked) {
        Intent intent = new Intent(activity, MusicService.class);
        intent.setAction("TOGGLE_PURE_RANDOM");
        intent.putExtra("STATE", isChecked);
        activity.startService(intent);

        uiController.applyMadnessTheme(isChecked); // Triggers the "Madness" UI change
    }
}
