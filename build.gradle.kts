@file:Suppress("UnstableApiUsage", "unused", "UNUSED_VARIABLE")

plugins {
  kotlin("plugin.serialization") version "1.7.0" apply false
  id("org.jetbrains.kotlinx.kover") version "0.5.0"
}

buildscript {
  repositories {
    google()
    mavenCentral()
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    maven("https://plugins.gradle.org/m2/")
  }
  dependencies {
    classpath("io.micronaut.gradle:micronaut-gradle-plugin:${Versions.micronautPlugin}")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    classpath("org.jetbrains.kotlinx:kover:${Versions.koverPlugin}")
    classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${Versions.atomicfuPlugin}")
  }
}


allprojects {
  repositories {
    google()
    mavenCentral()
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
    }
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
      jvmTarget = Versions.javaLanguage
      javaParameters = true
    }
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
    }
  }
}