plugins {
    kotlin("multiplatform")
}

kotlin {
    // Windows'ta hızlı test için JVM hedefi.
    jvm()
    // Kotlin/Native doğrulama hedefi: iOS ile aynı derleyici arka ucunu kullanır,
    // Windows'ta çalışır. commonMain'e yanlışlıkla JVM'e özgü API sızmasını yakalar.
    mingwX64()

    // Asıl hedefler — yalnızca macOS'ta (ör. Codemagic) derlenir.
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { hedef ->
        hedef.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
