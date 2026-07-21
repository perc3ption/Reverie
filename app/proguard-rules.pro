# Reverie release R8 / ProGuard rules

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Room entities / DAOs (generated Impl kept via Room consumer rules; entities need reflection)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class ** { *; }
-keep @androidx.room.Dao interface ** { *; }

# Media3 / ExoPlayer (reflection + service bindings)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# jaudiotagger uses reflective tag field access
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Keep PlaybackService for manifest / MediaSession
-keep class com.perceptiveus.reverie.playback.PlaybackService { *; }
