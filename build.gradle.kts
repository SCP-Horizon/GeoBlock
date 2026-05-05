plugins {
    id("java-library")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
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
