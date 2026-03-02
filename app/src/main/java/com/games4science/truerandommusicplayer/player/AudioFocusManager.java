package com.games4science.truerandommusicplayer.player;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

public class AudioFocusManager {

    public interface AudioFocusCallback {
        void onPlay();
        void onPause();
        void onStop();
        void onDuck();
        void onUnduck();
    }

    private final AudioManager audioManager;
    private final AudioFocusCallback callback;

    private AudioFocusRequest audioFocusRequest;
    private boolean isFocusGranted = false;
    private boolean shouldResumeAfterFocusGain = false;

    public AudioFocusManager(Context context, AudioFocusCallback callback) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.callback = callback;
        initFocusRequest();
    }

    private void initFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(this::onAudioFocusChange)
                    .build();
        }
    }

    public boolean requestFocus() {

        int result;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(
                    this::onAudioFocusChange,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );
        }

        isFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return isFocusGranted;
    }

    public void abandonFocus() {
        if (!isFocusGranted) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this::onAudioFocusChange);
        }

        isFocusGranted = false;
    }

    private void onAudioFocusChange(int focusChange) {

        switch (focusChange) {

            case AudioManager.AUDIOFOCUS_GAIN:
                if (shouldResumeAfterFocusGain) {
                    callback.onPlay();
                    shouldResumeAfterFocusGain = false;
                }
                callback.onUnduck();
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                callback.onStop();
                shouldResumeAfterFocusGain = false;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                shouldResumeAfterFocusGain = true;
                callback.onPause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                callback.onDuck();
                break;
        }
    }
}