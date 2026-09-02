import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
    id("com.google.gms.google-services")
}

// Yayın imzalama bilgileri keystore.properties'ten okunur (git'e girmez, bkz. .gitignore).
// Dosya yoksa (ör. CI'da henüz kurulmadıysa) release derlemesi imzasız kalır; assembleDebug
// bundan etkilenmez.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.app"
    compileSdk = 36

    buildFeatures{
        viewBinding = true
        // AgentDebugLog / GlobalLessonData.debugLog gibi yerlerin BuildConfig.DEBUG ile
        // release'de kendini kapatabilmesi için gerekli (AGP 8'de varsayılan olarak kapalı).
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.numigo.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room şeması için
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
        // CI runner'larda ~/.android/debug.keystore varsayılan konumu ortam kurulumuna göre
        // değişebiliyor (AGP her seferinde farklı, kayıtsız bir sertifika üretebiliyor →
        // Google Sign-In DEVELOPER_ERROR). CI_DEBUG_KEYSTORE_PATH ortam değişkeni verildiğinde
        // konumu varsaymadan doğrudan o dosyayı kullan; yoksa AGP'nin normal varsayılanı geçerli.
        System.getenv("CI_DEBUG_KEYSTORE_PATH")?.let { ciDebugKeystorePath ->
            getByName("debug") {
                storeFile = file(ciDebugKeystorePath)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            // R8 açık: kullanılmayan kod atılır ve isimler karıştırılır. Bu olmadan
            // istemci tarafı kontroller (tek cihaz, enerji, plan, öğretmen kontrolü)
            // APK'da çıplak okunabiliyor ve kolayca yamalanabiliyordu.
            //
            // Yansımayla çalışan her şey (Firestore toObject, Gson) proguard-rules.pro
            // içinde korunuyor. Release derlemesi ilk kez alındığında kayıt/giriş,
            // ders akışı, sohbet ve satın alma yollarının elle test edilmesi gerekir.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    lint {
        // Release derlemesi lint'in "fatal" bulgularında durur. Şu an tek bir mevcut bulgu var:
        //   fragment_tutorial.xml → devamButton, backButton'a kısıtlanmış ama backButton
        //   bottomPanelID LinearLayout'unun içinde, yani kardeş değiller. ConstraintLayout
        //   bu kısıtı çalışma anında yok sayıyor ve buton 0dp genişlikte kalıyor.
        //
        // Bu gerçek bir UI hatası ve düzeltilmesi görünümü değiştireceği için ayrıca ele
        // alınmalı. Baseline, MEVCUT bulguları kaydeder; bundan sonra eklenen YENİ lint
        // hataları release derlemesini yine durdurur.
        //
        // Baseline'ı yenilemek için: ./gradlew updateLintBaseline
        baseline = file("lint-baseline.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
        exclude(group = "com.intellij", module = "annotations")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.databinding.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //navigationBar
    implementation(libs.material)

    //character animation
    implementation(libs.lottie)

    //bundle off
    //noinspection UseTomlInstead

    //gson verilerin kaydedilmesi için
    implementation("com.google.code.gson:gson:2.10.1")

    //reklam için
    implementation("com.google.android.gms:play-services-ads:23.1.0")

    // Reklam rızası (UMP / CMP). AB ve İngiltere'deki kullanıcılara reklam gösterilmeden
    // önce rıza formunun sunulması Google'ın "EU user consent" politikası gereği zorunlu;
    // eksikliği AdMob hesabının askıya alınmasına yol açabiliyor.
    // Formun içeriği AdMob konsolundan (Privacy & messaging) tanımlanmalıdır.
    implementation("com.google.android.ump:user-messaging-platform:3.0.0")

    //bu da bişi ama ne bilmiyom
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")


    implementation ("com.github.bumptech.glide:glide:4.16.0")

    // Firebase dependencies
    // Email link (passwordless) migration away from Dynamic Links requires newer Auth SDKs.
    // Use BoM 32.7.0 to stay compatible with Kotlin 1.9.x in this project.
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Video oynatma (ağ / galeri formatları için) — aynı sürüm kullanılmalı
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Foreground Service için (yükleme bildirimi + kota yok)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // In-App Review API
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Google Play Billing (mağaza satın almaları) — bkz. docs/SATIN_ALMA_ENTEGRASYONU.md
    implementation("com.android.billingclient:billing:8.0.0")

    // Kupa geçmişi grafiği için
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}