import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }

    plugins {
        kotlin("jvm") version "2.1.0"
        id("org.jetbrains.intellij.platform") version "2.13.1"
        id("org.jetbrains.intellij.platform.settings") version "2.13.1"
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

rootProject.name = "Codex_bridge"