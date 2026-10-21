plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "top.mrxiaom"
version = "1.0.4"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("org.jetbrains:annotations:26.1.0")
    implementation("de.tr7zw:item-nbt-api:2.16.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.1")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    testImplementation("net.dmulloy2:ProtocolLib:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
val targetJavaVersion = 21
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
}
tasks {
    register<Jar>("compatibilityProbeJar") {
        dependsOn(testClasses)
        archiveClassifier.set("compatibility-probe")
        destinationDirectory.set(layout.buildDirectory.dir("compatibility-probe"))
        from(sourceSets.test.get().output) {
            include("**/ServerCompatibilityProbe*.class")
        }
        from("src/test/compat-plugin.yml") {
            rename { "plugin.yml" }
        }
    }
    test {
        useJUnitPlatform()
    }
    jar {
        enabled = false
    }
    shadowJar {
        archiveClassifier.set("")
        relocate("de.tr7zw.changeme.nbtapi", "top.mrxiaom.hidemyarmors.nbtapi")
    }
    build {
        dependsOn(shadowJar)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
    processResources {
        filesMatching("plugin.yml") {
            expand(mapOf("version" to version))
        }
    }
}
