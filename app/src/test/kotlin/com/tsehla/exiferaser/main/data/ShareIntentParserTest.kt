package com.tsehla.exiferaser.main.data

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentParserTest {

    private fun clipDataWith(vararg uris: Uri, mimeTypes: Array<String> = arrayOf("image/jpeg")): ClipData {
        val description = ClipDescription("shared", mimeTypes)
        val clip = ClipData(description, ClipData.Item(uris.first()))
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        return clip
    }

    @Test
    fun `parse returns null for non-share action`() {
        val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("content://media/images/1") }
        assertNull(ShareIntentParser.parse(intent))
    }

    @Test
    fun `parse returns null for null action`() {
        assertNull(ShareIntentParser.parse(Intent()))
    }

    @Test
    fun `parse returns null for already consumed share intent`() {
        val uri = Uri.parse("content://media/images/1")
        val intent = Intent(Intent.ACTION_SEND).apply {
            clipData = clipDataWith(uri)
            putExtra(ShareIntentParser.EXTRA_SHARE_CONSUMED, true)
        }
        assertNull(ShareIntentParser.parse(intent))
    }

    @Test
    fun `parse extracts single uri from send clipData`() {
        val uri = Uri.parse("content://media/images/1")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            clipData = clipDataWith(uri)
        }
        assertEquals(listOf(uri), ShareIntentParser.parse(intent))
    }

    @Test
    fun `parse extracts multiple uris from send multiple clipData`() {
        val first = Uri.parse("content://media/images/1")
        val second = Uri.parse("content://media/images/2")
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            clipData = clipDataWith(first, second)
        }
        assertEquals(listOf(first, second), ShareIntentParser.parse(intent))
    }

    @Test
    fun `parse returns empty list when clipData has unsupported mime types`() {
        val uri = Uri.parse("content://media/images/1")
        val intent = Intent(Intent.ACTION_SEND).apply {
            clipData = clipDataWith(uri, mimeTypes = arrayOf("image/gif"))
        }
        assertEquals(emptyList<Uri>(), ShareIntentParser.parse(intent))
    }

    @Test
    fun `parse falls back to extra stream when clipData is absent`() {
        val uri = Uri.parse("content://media/images/1")
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        assertEquals(listOf(uri), ShareIntentParser.parse(intent))
    }

    @Test
    fun `parse falls back to extra stream list for send multiple`() {
        val first = Uri.parse("content://media/images/1")
        val second = Uri.parse("content://media/images/2")
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(first, second))
        }
        assertEquals(listOf(first, second), ShareIntentParser.parse(intent))
    }

    @Test
    fun `parse returns empty list when share has no uris at all`() {
        val intent = Intent(Intent.ACTION_SEND)
        assertEquals(emptyList<Uri>(), ShareIntentParser.parse(intent))
    }

    @Test
    fun `isShareAction recognizes send actions only`() {
        assertTrue(ShareIntentParser.isShareAction(Intent.ACTION_SEND))
        assertTrue(ShareIntentParser.isShareAction(Intent.ACTION_SEND_MULTIPLE))
        assertEquals(false, ShareIntentParser.isShareAction(Intent.ACTION_MAIN))
        assertEquals(false, ShareIntentParser.isShareAction(null))
    }
}
