plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
}

group = "com.jetbrains.rider.plugins.souder"
version = "0.2.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

dependencies {
    intellijPlatform {
        rider("2024.2.5")
        instrumentationTools()
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        pluginVersion.set("${project.version}")
        sinceBuild.set("233")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}
