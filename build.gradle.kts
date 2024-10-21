// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript{
    repositories{
        maven { url = uri("https://jitpack.io") }
    }
}
plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.jvm") version "1.8.10" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
}
