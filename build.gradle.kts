plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

group = "ext.ide.bridges"
version = "1.0-SNAPSHOT"

dependencies {
    intellijPlatform {
        local("/opt/idea")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id.set("ext.ide.bridges.codex")
        name.set("Codex AI Bridge")

        ideaVersion {
            sinceBuild.set("253")
            untilBuild.set("253.*")
        }
    }
}