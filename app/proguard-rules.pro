# ProGuard / R8 size optimization rules for Siri Autonomous Assistant

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep Jetpack Compose models & components
-keep class androidx.compose.** { *; }
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }

# Keep Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Gson models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all Assistant App Classes
-keep class com.assistant.ai.** { *; }
-keepclassmembers class com.assistant.ai.** { *; }
