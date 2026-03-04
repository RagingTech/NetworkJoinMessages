plugins {
    `java`
    // Shade plugin
    id("com.gradleup.shadow") version "8.3.8"
    // Blossom plugin for Pebble templating
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
    id("net.kyori.blossom") version "2.1.0"
}

group = property("group") as String
version = property("version") as String
description = property("description") as String

// Dependency versions
val lombokVersion = "1.18.42"
val bstatsVersion = "3.0.2"
val adventureVersion = "4.24.0"
val gsonVersion = "2.13.2"
val h2Version = "2.2.224"
val velocityVersion = "3.4.0-SNAPSHOT"

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
    // Elytrium: local jar used as maven repo is down
    flatDir { dirs("libs") }
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
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$velocityVersion")
    compileOnly("com.velocitypowered:velocity-proxy:$velocityVersion") {
        exclude(group = "com.velocitypowered", module = "velocity-proxy-log4j2-plugin")
    }

    // JDBC drivers
    implementation("com.mysql:mysql-connector-j:9.6.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    implementation("org.postgresql:postgresql:42.7.9")

    implementation("com.h2database:h2:${h2Version}")

    // bStats
    implementation("org.bstats:bstats-bungeecord:$bstatsVersion")
    implementation("org.bstats:bstats-velocity:$bstatsVersion")

    // Utilities
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.jetbrains:annotations:16.0.1")

    // Adventure & MiniMessage
    implementation("net.kyori:adventure-platform-bungeecord:4.4.1")
    implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")

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

    testImplementation("net.luckperms:api:5.4")
    testImplementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    testImplementation("io.github.miniplaceholders:miniplaceholders-api:3.0.1")
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
            }
        }
    }
}

// Exclude legacy JUnit 4 from test runtime only
configurations.testRuntimeClasspath {
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
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
