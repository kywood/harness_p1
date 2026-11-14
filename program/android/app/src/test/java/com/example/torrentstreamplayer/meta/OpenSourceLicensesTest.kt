package com.example.torrentstreamplayer.meta

import com.example.torrentstreamplayer.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OpenSourceLicensesTest {
    private val raw = File("src/main/res/raw")

    @Test fun everyLicenseDocumentHasItsRawFile() {
        assertTrue("res/raw not found", raw.isDirectory)
        LicenseDocument.entries.forEach { document ->
            val file = File(raw, document.fileName + ".txt")
            assertTrue(file.path, file.isFile && file.length() > 0)
        }
    }

    @Test fun everyRawLicenseFileIsUsed() {
        val files = raw.listFiles { file -> file.name.startsWith("license_") }!!.map { it.nameWithoutExtension }.toSet()
        assertEquals(files, LicenseDocument.entries.map { it.fileName }.toSet())
        assertEquals(LicenseDocument.entries.toSet(), OpenSourceComponent.entries.map { it.document }.toSet())
    }

    @Test fun copyleftLicensesOfferSourceAndLinkToIt() {
        val copyleft = setOf(LicenseDocument.LGPL_2_1, LicenseDocument.MPL_2_0)
        OpenSourceComponent.entries.forEach { component ->
            assertEquals(component.name, component.document in copyleft, component.sourceOffer)
            assertTrue(component.name, component.url.startsWith("https://"))
        }
        assertEquals(listOf(OpenSourceComponent.LIBVLC), OpenSourceComponent.entries.filter { it.lgplNotice })
        assertEquals("LGPL 항목이 맨 앞", OpenSourceComponent.LIBVLC, OpenSourceComponent.entries.first())
    }

    @Test fun listedVersionsMatchTheBuild() {
        val catalog = File("../gradle/libs.versions.toml").readText()
        val libvlc = OpenSourceComponent.LIBVLC.version.value
        assertTrue(catalog.contains("libvlc = \"$libvlc\""))
        assertTrue("자체 빌드 LGPL LibVLC", libvlc.contains("-lgpl."))
        assertTrue(catalog.contains("group = \"pieceplay.libvlc\""))
        assertTrue(catalog.contains("nanohttpd = \"${OpenSourceComponent.NANOHTTPD.version.value}\""))
        assertTrue(catalog.contains("version = \"${OpenSourceComponent.LIBTORRENT4J.version.value}\""))
        assertTrue(catalog.contains("kotlin = \"${OpenSourceComponent.KOTLIN.version.value}\""))
        val composeBom = Regex("composeBom = \"([^\"]+)\"").find(catalog)!!.groupValues[1]
        assertTrue(OpenSourceComponent.ANDROIDX.version.value.endsWith(composeBom))
    }

    /** LibVLC는 GPL 구성요소를 뺀 빌드라 GPL 고지가 남아 있으면 안 된다(program/libvlc-lgpl-phase4.md). */
    @Test fun noGplNoticeIsLeft() {
        assertFalse(File(raw, "license_gpl_2_0.txt").exists())
        OpenSourceComponent.entries.forEach { component ->
            assertFalse(component.name, component.licenseName.value.startsWith("GPL"))
        }
    }

    /** 소스 코드 링크는 이 앱이 쓰는 빌드의 소스를 공개한 저장소를 연다. */
    @Test fun libVlcSourceLinkIsThePublishedBuild() {
        assertEquals("https://github.com/kywood/pieceplay-libvlc", OpenSourceComponent.LIBVLC.url)
        val settings = File("../settings.gradle.kts").readText()
        assertTrue(settings.contains("https://github.com/kywood/pieceplay-libvlc/releases/download"))
    }

    @Test fun bundledComponentsNameAListedHost() {
        val hosts = OpenSourceComponent.entries.map { it.displayName.value }.toSet()
        val bundled = OpenSourceComponent.entries.map { it.version }.filterIsInstance<LicenseText.Localized>()
            .filter { it.format == R.string.licenses_version_bundled }
        assertTrue(bundled.isNotEmpty())
        bundled.forEach { assertTrue(it.value, it.value.substringBefore(' ') in hosts) }
    }

    @Test fun koreanTextLivesInStringResourcesNotInTheTable() {
        val hangul = Regex("[가-힣]")
        OpenSourceComponent.entries.forEach { component ->
            val values = listOf(component.displayName, component.version, component.licenseName).map { it.value } +
                listOf(component.url, component.copyright)
            values.forEach { assertFalse("${component.name}: $it", hangul.containsMatchIn(it)) }
        }
        val koreanLiteral = Regex("\"[^\"]*[가-힣][^\"]*\"")
        val code = File("src/main/java/com/example/torrentstreamplayer/meta/OpenSourceLicenses.kt").readLines()
            .filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("/**") }
            .filter { koreanLiteral.containsMatchIn(it) }
        assertEquals("한국어 문자열 리터럴", emptyList<String>(), code)
    }
}
