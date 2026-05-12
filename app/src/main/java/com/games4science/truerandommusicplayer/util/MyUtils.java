package com.games4science.truerandommusicplayer.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MyUtils {

    public static String formatTime(long ms) {

        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = null;

        // If it's a content URI (the most common for selected files)
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // If it's a file URI or the content query failed
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
            if (fileName != null && fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
        }

        return fileName != null ? fileName.replace("%20", " ") : "Unknown Song";
    }

    public static String generateMd5Token(String password, String salt) {
        try {
            String plainText = password + salt;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(plainText.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
