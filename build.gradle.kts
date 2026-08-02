plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.2.2"
}

group = "net.okocraft"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")

    compileOnly("net.luckperms:api:5.4")

    implementation("com.github.siroshun09.configapi:configapi-yaml:4.6.3")
    implementation("com.github.siroshun09.translationloader:translationloader:2.0.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks {
    compileJava {
        options.release.set(25)
    }

    processResources {
        filesMatching(listOf("plugin.yml", "en.yml", "ja_JP.yml")) {
            expand("projectVersion" to version)
        }
    }

    shadowJar {
        relocate("com.github.siroshun09", "${project.group}.${project.name.lowercase()}.lib")
        manifest {
            attributes("paperweight-mappings-namespace" to "mojang")
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set(project.name)
                description.set("TimedPerms, plugin to implements in-game time temporary permission using LuckPerms.")
                url.set("https://github.com/okocraft/timedperms")

                licenses {
                    license {
                        name.set("GNU General Public License, Version 3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/okocraft/timedperms.git")
                    developerConnection.set("scm:git:git@github.com:okocraft/timedperms.git")
                    url.set("https://github.com/okocraft/timedperms")
                }
            }
        }
    }
}
