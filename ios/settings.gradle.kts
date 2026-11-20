// Sorobit iOS — Android projesinden tamamen bağımsız Gradle kökü.
// Kök dizindeki settings.gradle.kts bu klasörü İÇERMEZ; Android derlemesi buradan etkilenmez.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "SorobitIos"
include(":shared")
