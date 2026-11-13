package com.rsdgt.mihon

import eu.kanade.tachiyomi.source.SourceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceManagerTest {

    @Test
    fun sourceManager_registersNHentaiApi() {
        val sourceManager = SourceManager.getInstance()
        val nhentaiSource = sourceManager.getNHentaiSource()

        assertNotNull("NHentaiApi should be registered and non-null", nhentaiSource)
        assertEquals("NHentai (API v2)", nhentaiSource.name)
        assertEquals("en", nhentaiSource.lang)
    }

    @Test
    fun sourceManager_listsActiveSources() {
        val sourceManager = SourceManager.getInstance()
        val sources = sourceManager.getSources()

        assertFalse("Sources list should not be empty", sources.isEmpty())
        val found = sources.any { it.name == "NHentai (API v2)" }
        assertTrue("NHentai source should be present in registered sources list", found)
    }
}
