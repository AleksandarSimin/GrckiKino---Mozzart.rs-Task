pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "7.3.1" apply false
        id("org.jetbrains.kotlin.android") version "1.7.10" apply false
        id("com.google.devtools.ksp") version "1.7.10-1.0.6" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GrckiKino"
include(":app")
include(":libs")