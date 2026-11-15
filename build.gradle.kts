
plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "uk.vrtxx.linked"
version = providers.gradleProperty("pluginVersion").orElse("1.0.0").get()
val minecraftVersion = "26.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

spotless {
    java {
        googleJavaFormat("1.35.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {
        val properties = mapOf(
            "version" to project.version.toString(),
            "minecraftVersion" to minecraftVersion
        )

        inputs.properties(properties)

        filesMatching("plugin.yml") {
            expand(properties)
        }
    }

    jar {
        archiveBaseName.set("linked")
    }

    runServer {
        minecraftVersion(minecraftVersion)
    }

    named("build") {
        dependsOn("spotlessCheck")
    }
}
