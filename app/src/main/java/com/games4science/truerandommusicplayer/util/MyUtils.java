package com.games4science.truerandommusicplayer.util;

public class MyUtils {

    public static String formatTime(long ms) {

        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

}
