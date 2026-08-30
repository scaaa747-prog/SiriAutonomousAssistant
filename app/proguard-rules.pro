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

# Keep Accessibility Service & System Models
-keep class com.assistant.ai.accessibility.** { *; }
-keep class com.assistant.ai.agent.** { *; }
-keep class com.assistant.ai.core.** { *; }
-keep class com.assistant.ai.data.** { *; }
-keep class com.assistant.ai.model.** { *; }
