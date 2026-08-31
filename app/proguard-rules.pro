# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Room handles its own proguard rules mostly, but we keep generic entities
-keep class com.example.puffwheel.data.db.** { *; }

# Media3/ExoPlayer
-keep class androidx.media3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep UI classes to prevent issues with ViewBinding/Fragments via reflection
-keep class com.example.puffwheel.ui.** { *; }
