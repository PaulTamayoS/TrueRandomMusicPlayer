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

# Keep line numbers for Play Store crash reports
-keepattributes SourceFile, LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Media3 / ExoPlayer core rules
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }

# Prevent R8 from stripping the "Binder" methods for the MusicService
# This ensures your Activity can always connect to the MediaSession
-keepclassmembers class * extends androidx.media3.session.MediaSessionService {
    public <init>();
}

# Keep Retrofit and OkHttp internal logic
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class com.games4science.truerandommusicplayer.api.** { *; }
-keep interface com.games4science.truerandommusicplayer.api.** { *; }

# Retrofit specific rules
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

# GSON specific rules
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**