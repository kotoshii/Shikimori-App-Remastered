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
-dontskipnonpubliclibraryclassmembers
-dontpreverify

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
# Kotlin 1.4's stdlib jars carry META-INF/versions/9/module-info.class, which ProGuard 6.0.3 stops on as a
# wrongly named class; Android has no use for it, so it is left out
-dontwarn module-info
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
-keep public class * extends androidx.preference.PreferenceFragmentCompat

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
