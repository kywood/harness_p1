package com.example.torrentstreamplayer.meta

import org.junit.Assert.assertEquals
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
        val copyleft = setOf(LicenseDocument.LGPL_2_1, LicenseDocument.GPL_2_0, LicenseDocument.MPL_2_0)
        OpenSourceComponent.entries.forEach { component ->
            assertEquals(component.name, component.document in copyleft, component.sourceOffer)
            assertTrue(component.name, component.url.startsWith("https://"))
        }
        assertEquals(listOf(OpenSourceComponent.LIBVLC), OpenSourceComponent.entries.filter { it.lgplNotice })
        assertEquals("LGPL 항목이 맨 앞", OpenSourceComponent.LIBVLC, OpenSourceComponent.entries.first())
    }

    @Test fun listedVersionsMatchTheBuild() {
        val catalog = File("../gradle/libs.versions.toml").readText()
        assertTrue(catalog.contains("libvlc = \"${OpenSourceComponent.LIBVLC.version}\""))
        assertTrue(catalog.contains("nanohttpd = \"${OpenSourceComponent.NANOHTTPD.version}\""))
        assertTrue(catalog.contains("version = \"${OpenSourceComponent.LIBTORRENT4J.version}\""))
        assertTrue(catalog.contains("kotlin = \"${OpenSourceComponent.KOTLIN.version.substringBefore(' ')}\""))
        assertTrue(OpenSourceComponent.ANDROIDX.version.endsWith(Regex("composeBom = \"([^\"]+)\"").find(catalog)!!.groupValues[1]))
    }
}
