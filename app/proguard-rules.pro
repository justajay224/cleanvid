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

# Aturan umum untuk library Google HTTP Client dan Jackson JSON
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-keep class com.google.api.client.json.GenericJson {
  <fields>;
}

# Aturan spesifik untuk Data Class Anda
-keep class com.wannabe.cleanvid.SpamComment { *; }

# =================================================================
# PENAMBAHAN BARU YANG PENTING
# Melindungi semua kelas model data dari YouTube API agar tidak diacak
-keep class com.google.api.services.youtube.model.** { *; }
# =================================================================

# Aturan tambahan untuk menghindari warning saat build
-dontwarn com.google.api.client.googleapis.googleapi.**
-dontwarn com.google.api.client.util.GenericData
