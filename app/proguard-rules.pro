# ================= GENERAL =================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# ================= KOTLIN =================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ================= KOTLINX SERIALIZATION =================
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.guard.screen.**$$serializer { *; }
-keepclassmembers class com.guard.screen.** {
    *** Companion;
}
-keepclasseswithmembers class com.guard.screen.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ================= HILT =================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint

# ================= SUPABASE =================
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**

# ================= OKHTTP =================
-dontwarn okhttp3.**
-dontwarn okio.**

# ================= GSON =================
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ================= MODELS =================
-keep class com.guard.screen.data.model.** { *; }

# ================= TIMBER =================
-dontwarn org.jetbrains.annotations.**
