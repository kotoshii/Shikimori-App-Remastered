# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Gson builds the api's response classes (and the saved login) by reflection, through no constructor R8
# can see, so R8 takes them for never made: it empties them and turns the code that reads them into a
# throw - the converter of every user's avatars did, in the first R8 build (P7-T16). ProGuard never did
# this. Every class with a @SerializedName field is kept as made, with those fields; both may still be
# renamed, as before, since Gson reads the names from the annotation. Gson 2.11+ ships a rule like it.
-keepclasseswithmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn kotlinx.atomicfu.**
-dontnote android.net.http.**
-dontnote org.apache.http.**
-dontwarn com.google.android.material.snackbar.**
-dontwarn io.lindstrom.m3u8.**

# Jsoup 1.14+ names jsr305's annotations (@Nullable, @ParametersAreNonnullByDefault), which no app ships:
# without this ProGuard stops on 141 warnings. It only silences them; Jsoup is kept whole below.
-dontwarn javax.annotation.**
-keep class org.jsoup.**

-keepclassmembers enum * { *; }

# AGP 3.2's default rules kept every field of every R class; AGP 4.1 dropped that rule. Joda-Time-Android
# finds its time zone data by reading net.danlew.android.joda.R$raw's fields by name, which ProGuard cannot
# see, so without this line the release build crashed on start ("Could not read ZoneInfoMap"). Kept for every
# R class, as before, since a library can read R by name without saying so.
-keepclassmembers class **.R$* {
    public static <fields>;
}

-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-dontwarn org.slf4j.**
-keep public class * extends androidx.preference.PreferenceFragmentCompat { <init>(); }

# HostingFilterFragment is a plain Fragment, so the rule above does not cover it, and the only thing
# naming it is preferences_anime.xml. ProGuard dropped it from the release build and opening the
# screen crashed the app. Anything else reached by app:fragment needs a line here too.
-keep public class com.gnoemes.shikimori.presentation.view.settings.fragments.HostingFilterFragment { <init>(); }

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
