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
    setupJUnit(libs.junit.bom)
    setupMockito(libs.mockito)

    commonRepositories {
        maven("https://repo.extendedclip.com/releases/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    commonDependencies {
        compileOnly(libs.paper.api)
        compileOnly(libs.placeholderapi)
        compileOnly(libs.luckperms.api)

        compileOnly(libs.configurate.yaml)
        implementation(libs.mcmsgdef)
        testImplementation(libs.junit.jupiter)
        testImplementation(libs.mockito)
        testImplementation(libs.paper.api)
        testImplementation(libs.luckperms.api)
        testImplementation(libs.configurate.yaml)
    }
}

bundler {
    copyToRootBuildDirectory("TimedPerms-${project.version}")
    replacePluginVersionForBukkit(project.version)
}

tasks.shadowJar {
    relocate("dev.siroshun.mcmsgdef", "${project.group}.${project.name.lowercase()}.lib.mcmsgdef")
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

tasks.test {
    dependsOn(tasks.shadowJar)
    systemProperty("verification.pluginJar", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
}
