# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Firebase classes
-keep class com.google.firebase.** { *; }

# Keep Agora classes
-keep class io.agora.** { *; }

# Keep data models
-keep class com.sketchsync.data.model.** { *; }
