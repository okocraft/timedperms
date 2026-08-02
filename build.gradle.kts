plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.jcommon)
    alias(libs.plugins.bundler)
}

group = "net.okocraft"
version = "1.0.0"

jcommon {
    javaVersion = JavaVersion.VERSION_25

    setupPaperRepository()

    commonRepositories {
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    commonDependencies {
        compileOnly(libs.paper.api)
        compileOnly(libs.placeholderapi)
        compileOnly(libs.luckperms.api)

        implementation(libs.configapi.yaml)
        implementation(libs.translationloader)
    }
}

bundler {
    copyToRootBuildDirectory("TimedPerms-${project.version}")
    replacePluginVersionForBukkit(project.version)
}

tasks.shadowJar {
    relocate("com.github.siroshun09", "${project.group}.${project.name.lowercase()}.lib")
    manifest {
        attributes("paperweight-mappings-namespace" to "mojang")
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
