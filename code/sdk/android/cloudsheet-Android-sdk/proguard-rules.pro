# CloudSheetSDK ProGuard Rules

# Keep all public API classes and methods
-keep public class com.opencloudsheet.sdk.** {
    public *;
}

# Keep MSAL classes
-keep class com.microsoft.identity.** { *; }
-dontwarn com.microsoft.identity.**

# Keep attributes for proper stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Keep generic signatures for Kotlin
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
