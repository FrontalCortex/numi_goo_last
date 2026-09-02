# ════════════════════════════════════════════════════════════════════════════
# R8 / ProGuard kuralları
#
# Release derlemesinde `isMinifyEnabled = true` açıldı. R8 kullanılmayan kodu atar ve
# sınıf/metot adlarını karıştırır. Bu iki davranış, YANSIMA (reflection) ile çalışan
# kütüphaneleri bozar — Firestore'un `toObject()`'i ve Gson alan adlarını yansımayla
# okur, dolayısıyla o model sınıflarının adları ve alanları korunmalıdır.
#
# Bir şey release'de bozulursa: `app/build/outputs/mapping/release/mapping.txt`
# dosyası, karıştırılmış yığın izlerini (stack trace) çözmek için kullanılır — sakla.
# ════════════════════════════════════════════════════════════════════════════

# ── Yığın izleri okunabilir kalsın ──────────────────────────────────────────
# Kaynak dosya adını gizle ama satır numarasını koru: Crashlytics/Play Console
# çökme raporları böylece anlamlı kalır.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin ve genel yansıma için gerekli üstveri
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ── Uygulamanın veri modelleri ──────────────────────────────────────────────
# Firestore `toObject()` ve Gson `fromJson()` bu sınıfları YANSIMAYLA kurar:
# alan adları karıştırılırsa Firestore/JSON alanlarıyla eşleşmez ve sessizce
# null/0 dönerler. Bu yüzden model paketinin tamamı ve JSON'a serileştirilen
# üst seviye data class'lar korunuyor.
-keep class com.example.app.model.** { *; }
-keep class com.example.app.DailyQuestionChallenge { *; }
-keep class com.example.app.DailyQuestionChallenge$* { *; }
-keep class com.example.app.PendingUploadMeta { *; }
-keep class com.example.app.BadgeLevelUpPayload { *; }

# Firestore/Gson ile kullanılan tüm data class'lar için güvenlik ağı:
# no-arg constructor ve alanlar korunmalı.
-keepclassmembers class com.example.app.** {
    <init>();
    <fields>;
}

# ── Kotlin ─────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Gson ───────────────────────────────────────────────────────────────────
# TypeToken jenerik bilgisini çalışma anında okur; Signature attribute'u şart.
-dontwarn sun.misc.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Firebase ───────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
# Firestore'un yansımayla kurduğu POJO'lar için no-arg constructor'lar
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
}

# ── Google Play Billing ────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── Media3 / ExoPlayer ─────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Glide ──────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# ── Lottie ─────────────────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ── MPAndroidChart ─────────────────────────────────────────────────────────
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ── Play Review / Play Core ────────────────────────────────────────────────
-keep class com.google.android.play.** { *; }
-dontwarn com.google.android.play.**

# ── Android bileşenleri ────────────────────────────────────────────────────
# Manifest'ten adla örneklenirler; karıştırılmamalılar.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment

# XML layout'lardan kurulan özel View'lar (CircleProgressBar, GuidePanelView,
# VerticalSliderView vb.) — inflater bunları adla ve (Context, AttributeSet)
# constructor'ıyla bulur.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    public *** get*();
}

# XML'deki android:onClick
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Enum'ların values()/valueOf()'u yansımayla çağrılabilir
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable CREATOR alanları
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ── Release'de log gürültüsünü at ──────────────────────────────────────────
# Uygulamada 1000'den fazla Log.d/Log.v çağrısı var; release'de hem gereksiz iş
# yapıyorlar hem de teşhis bilgisini logcat'e sızdırıyorlar. R8 bunları argüman
# hesaplamasıyla birlikte tamamen kaldırır.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
