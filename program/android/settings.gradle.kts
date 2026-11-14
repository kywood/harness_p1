pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // LibVLC built without GPL components (program/libvlc-lgpl-phase4.md). The AAR is a GitHub Release asset of
        // the public source repository; its SHA-256 is pinned in gradle/verification-metadata.xml.
        exclusiveContent {
            forRepository {
                ivy {
                    name = "PiecePlayLibVlcReleases"
                    url = java.net.URI.create("https://github.com/kywood/pieceplay-libvlc/releases/download")
                    patternLayout { artifact("[revision]/[artifact]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeGroup("pieceplay.libvlc") }
        }
        google()
        mavenCentral()
        maven { url = java.net.URI.create("https://jitpack.io") }
    }
}

rootProject.name = "PiecePlay"
include(":app")
