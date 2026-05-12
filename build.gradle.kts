plugins {
    id("java-library")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
    alias(libs.plugins.minotaur)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)

    implementation(libs.geoip2)
    implementation(libs.caffeine)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G")
        pluginJars(shadowJar.flatMap { it.archiveFile })
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        val shadedBase = "fr.horizonsmp.geoBlock.lib"
        relocate("com.maxmind", "$shadedBase.maxmind")
        relocate("com.github.benmanes.caffeine", "$shadedBase.caffeine")
        relocate("com.fasterxml.jackson", "$shadedBase.jackson")
        relocate("org.checkerframework", "$shadedBase.checkerframework")
        relocate("com.google.errorprone", "$shadedBase.errorprone")
        mergeServiceFiles()
    }

    jar {
        archiveClassifier.set("plain")
    }

    build {
        dependsOn(shadowJar)
    }
}

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set("geoblock")
    versionNumber.set(project.version.toString())
    versionType.set(resolveVersionType(project.version.toString()))
    uploadFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    gameVersions.addAll("1.21.4", "1.21.5", "1.21.6")
    loaders.addAll("paper", "purpur")
    changelog.set(provider { extractChangelogSection(project.version.toString()) })
    syncBodyFrom.set(provider { file("README.md").readText() })
    dependencies {
        optional.project("luckperms")
    }
}

fun resolveVersionType(version: String): String = when {
    version.contains("alpha") -> "alpha"
    version.contains("beta") -> "beta"
    else -> "release"
}

fun extractChangelogSection(version: String): String {
    val file = file("CHANGELOG.md")
    if (!file.exists()) return ""
    val out = StringBuilder()
    var capture = false
    for (line in file.readLines()) {
        if (line.startsWith("## ")) {
            if (capture) break
            capture = line.contains(version)
            continue
        }
        if (capture) out.appendLine(line)
    }
    return out.toString().trim()
}
