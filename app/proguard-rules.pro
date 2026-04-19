# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile



# 1. Media3 / ExoPlayer core rules
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }

# 2. Prevent R8 from stripping the "Binder" methods for the MusicService
# This ensures your Activity can always connect to the MediaSession
-keepclassmembers class * extends androidx.media3.session.MediaSessionService {
    public <init>();
}

# 3. Preserve your Repository and Data keys
# Since you use JSON strings (like "uri", "title"), we want to make sure
# the underlying reflection used by some libraries doesn't break.
-keepattributes Signature, *Annotation*, EnclosingMethod

# 4. (Optional but Recommended) Keep line numbers for Play Store crash reports
-keepattributes SourceFile, LineNumberTable
