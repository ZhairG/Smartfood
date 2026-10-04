# Reglas de ProGuard / R8 para Smartfood (Modo Completo)

# === Kotlin Serialization (Navegacion Type-Safe) ===
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}
-keep class com.smartfood.app.core.navigation.Route** { *; }

# === Dagger Hilt & ViewModel ===
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel
-keep class com.smartfood.app.core.di.** { *; }

# === Firebase Auth & Google Play Services ===
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# === Kotlin Coroutines ===
-keepclassmembers class kotlinx.coroutines.** { *; }
