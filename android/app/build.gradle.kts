import java.util.Properties
import java.time.LocalDate
import java.time.ZoneId
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

object ReleaseVersioning {
    fun next(previous: Properties, today: LocalDate): Properties {
        name(previous)
        val date = LocalDate.parse(previous.getProperty("date"))
        require(!today.isBefore(date)) { "Version date cannot move backwards" }
        val revision = if (today == date) previous.getProperty("revision").toInt() + 1 else 1
        val code = previous.getProperty("code").toInt()
        require(code in 1 until Int.MAX_VALUE) { "Version code is out of range" }
        return Properties().apply {
            setProperty("date", today.toString()); setProperty("revision", revision.toString())
            setProperty("code", (code + 1).toString()); name(this)
        }
    }
    fun name(properties: Properties): String {
        val date = LocalDate.parse(properties.getProperty("date"))
        val revision = properties.getProperty("revision").toInt()
        require(revision in 1..999) { "Version revision must be 1..999" }
        return "v." + date.toString().replace('-', '.') + "." + revision.toString().padStart(3, '0')
    }
}
val releaseProperties = Properties().apply { file("version.properties").inputStream().use { load(it) } }
val releaseName = ReleaseVersioning.name(releaseProperties)
val releaseCode = releaseProperties.getProperty("code").toInt().also { require(it > 0) }
val uploadProperties = Properties().apply {
    val propertiesFile = rootProject.file(".signing/upload.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.example.torrentstreamplayer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.gamenuri.pieceplay"
        minSdk = 26
        targetSdk = 37
        versionCode = releaseCode
        versionName = releaseName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("upload") {
            if (uploadProperties.isNotEmpty()) {
                storeFile = rootProject.file(uploadProperties.getProperty("storeFile"))
                storePassword = uploadProperties.getProperty("storePassword")
                keyAlias = uploadProperties.getProperty("keyAlias")
                keyPassword = uploadProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("upload")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.libvlc)
    implementation(libs.nanohttpd)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.libtorrent4j)
    implementation(libs.libtorrent4j.arm64)
    implementation(libs.libtorrent4j.arm)
    implementation(libs.libtorrent4j.x86)
    implementation(libs.libtorrent4j.x64)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
val bumpRequested = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "nextVersion" }
require(!bumpRequested || gradle.startParameter.taskNames.size == 1) { "Run :app:nextVersion separately, then build." }
tasks.register("nextVersion") {
    group = "versioning"
    description = "Advance the Seoul date/revision and Android version code."
    val target = layout.projectDirectory.file("version.properties").asFile
    doLast {
        val current = Properties().apply { target.inputStream().use { load(it) } }
        val next = ReleaseVersioning.next(current, LocalDate.now(ZoneId.of("Asia/Seoul")))
        val temporary = File(target.parentFile, ".version.properties.tmp")
        temporary.outputStream().use { next.store(it, "PiecePlay release version") }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        logger.lifecycle("Next version: " + ReleaseVersioning.name(next) + " (code " + next.getProperty("code") + ")")
    }
}
tasks.register("verifyVersioning") {
    group = "verification"
    doLast {
        fun sample(date: String, revision: Int = 1) = Properties().apply {
            setProperty("date", date); setProperty("revision", revision.toString()); setProperty("code", "11")
        }
        val first = sample("2026-11-01")
        check(ReleaseVersioning.name(first) == "v.2026.11.01.001")
        val second = ReleaseVersioning.next(first, LocalDate.parse("2026-11-01"))
        check(ReleaseVersioning.name(second) == "v.2026.11.01.002" && second.getProperty("code") == "12")
        check(ReleaseVersioning.name(ReleaseVersioning.next(second, LocalDate.parse("2026-11-02"))) == "v.2026.11.02.001")
        check(runCatching { ReleaseVersioning.next(first, LocalDate.parse("2026-10-31")) }.isFailure)
        check(runCatching { ReleaseVersioning.next(sample("2026-11-01", 999), LocalDate.parse("2026-11-01")) }.isFailure)
        logger.lifecycle("Versioning checks passed.")
    }
}
tasks.register<Copy>("packageReleaseFiles") {
    group = "distribution"
    dependsOn("assembleRelease", "bundleRelease")
    val stem = "PiecePlay-" + releaseName + "-" + releaseCode
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk")) { rename { stem + ".apk" } }
    from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab")) { rename { stem + ".aab" } }
    into(layout.buildDirectory.dir("outputs/pieceplay-release"))
}
