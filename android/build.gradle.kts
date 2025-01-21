plugins {
    kotlin("jvm") version "1.9.20"
}

buildscript {
    val agp_version by extra("8.5.2")
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$agp_version")
        classpath("com.google.gms:google-services:4.4.1")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0-Beta4")
    }
}