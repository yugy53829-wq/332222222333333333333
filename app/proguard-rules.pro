# SmartKey Clip ProGuard / R8 Keep Rules

# Preserve source file & line numbers for crash diagnostics (without exposing secrets)
-keepattributes SourceFile,LineNumberTable

# Moshi & Retrofit DTOs
-keepclassmembers class com.example.telegram.** { *; }
-keepclassmembers class com.example.data.*Json* { *; }

# Keep Android InputMethodService entry point
-keep public class com.example.ime.SmartKeyboardImeService
