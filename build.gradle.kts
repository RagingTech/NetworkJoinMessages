plugins {
    `java`
    `maven-publish`
    // Shade plugin
    id("com.gradleup.shadow") version "8.3.8"
}

group = property("group") as String
version = property("version") as String
description = property("description") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    // Sonatype
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    // PaperMC
    maven("https://repo.papermc.io/repository/maven-public/")
    // Jitpack
    maven("https://jitpack.io")
    // William278
    maven("https://repo.william278.net/releases/")
    // Elytrium
    maven("https://maven.elytrium.net/repo/")
    // SayanDevelopment
    maven("https://repo.sayandev.org/snapshots") {
        content {
            includeGroup("org.sayandev")
        }
    }
}

dependencies {
    // Proxy APIs
    compileOnly("net.md-5:bungeecord-api:1.21-R0.2")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-proxy:3.4.0-SNAPSHOT") {
        exclude(group = "com.velocitypowered", module = "velocity-proxy-log4j2-plugin")
    }

    // bStats
    implementation("org.bstats:bstats-bungeecord:3.0.2")
    implementation("org.bstats:bstats-velocity:3.0.2")

    // Utilities
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.jetbrains:annotations:16.0.1")
    implementation("com.h2database:h2:2.2.224")

    // Adventure & MiniMessage
    implementation("net.kyori:adventure-platform-bungeecord:4.4.1")
    implementation("net.kyori:adventure-text-minimessage:4.24.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.24.0")

    // MiniPlaceholders
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.0.1")

    // SayanVanish
    compileOnly("org.sayandev:sayanvanish-api:1.7.0-SNAPSHOT")
    // PremiumVanish
    compileOnly("com.github.LeonMangler:PremiumVanishAPI:2.9.18-2")
    // PAPIProxyBridge
    compileOnly("net.william278:papiproxybridge:1.8")
    // LuckPerms
    compileOnly("net.luckperms:api:5.4")

    // Boosted Yaml
    implementation("dev.dejvokep:boosted-yaml:1.3.6")
    // Discord Webhooks
    implementation("com.github.EarthCow:JavaDiscordWebhook:1.2.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mockito
    testImplementation("org.mockito:mockito-core:5.19.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.19.0")

    testImplementation("com.google.code.gson:gson:2.13.2")
    testImplementation("org.jetbrains:annotations:16.0.1")
    testImplementation("com.h2database:h2:2.2.224")

    testImplementation("net.luckperms:api:5.4")
    testImplementation("net.kyori:adventure-text-serializer-plain:4.24.0")
    testImplementation("io.github.miniplaceholders:miniplaceholders-api:3.0.1")
}

configurations.all {
    exclude(group = "junit", module = "junit")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "group" to project.group,
            "url" to project.property("url"),
            "mainClass" to project.property("mainClass"),
            "author" to project.property("author")
        )
    }
}

tasks.shadowJar {
    // Overwrite default jar
    archiveClassifier.set("")
    relocate("org.bstats", "xyz.earthcow.networkjoinmessages.libs.bstats")
    relocate("dev.dejvokep.boostedyaml", "xyz.earthcow.networkjoinmessages.libs.boostedyaml")

    dependencies {
        exclude(dependency("io.github.miniplaceholders:miniplaceholders-api"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
