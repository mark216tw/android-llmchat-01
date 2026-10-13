# ---------------------------------------------------------------------------
# R8 / ProGuard rules for the `prerelease` build type.
# ---------------------------------------------------------------------------

# --- kotlinx.serialization ---------------------------------------------------
# Keep the generated serializers and the synthetic Companion.serializer()
# accessors that the runtime looks up reflectively.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.llmchat.**$$serializer { *; }
-keepclassmembers class com.example.llmchat.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.llmchat.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit ----------------------------------------------------------------
# Retrofit reads HTTP annotations and generic signatures at runtime.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keepclasseswithmembers,allowshrinking interface * {
    @retrofit2.http.* <methods>;
}

# --- androidx.security: security-crypto pulls in Tink, which references
# optional Error Prone annotations that are not on the runtime classpath. ----
-dontwarn com.google.errorprone.annotations.**

# --- OkHttp / okhttp-sse -----------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Room --------------------------------------------------------------------
# Room ships its own consumer rules; nothing extra required.

# --- Hilt / Dagger -----------------------------------------------------------
# Hilt ships its own consumer rules; keep generated components defensively.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
