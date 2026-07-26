package com.tsehla.exiferaser.core.extension

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriExtensionsExtractImageIdTest {

    @Test
    fun `photo picker uri yields its numeric picker id`() {
        val uri = Uri.parse("content://media/picker/0/com.tsehla.exiferaser/12604")

        assertEquals("12604", uri.extractImageId())
    }

    @Test
    fun `shared media store uri yields its numeric media id`() {
        val uri = Uri.parse("content://media/external/images/media/80")

        assertEquals("80", uri.extractImageId())
    }

    @Test
    fun `media uri without a numeric last segment yields null`() {
        val uri = Uri.parse("content://media/external/images/media/IMG_2026.jpg")

        assertNull(uri.extractImageId())
    }

    @Test
    fun `media documents uri with numeric document id yields that id`() {
        val uri = Uri.parse(
            "content://com.android.providers.media.documents/document/images_media%3A12636",
        )

        assertEquals("12636", uri.extractImageId())
    }

    @Test
    fun `externalstorage document uri with file path yields null`() {
        val uri = Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FIMG_2026.jpg",
        )

        assertNull(uri.extractImageId())
    }

    @Test
    fun `camera file provider uri yields null`() {
        val uri = Uri.parse(
            "content://com.tsehla.exiferaser.fileprovider/camera/IMG_20261112_211439.jpg",
        )

        assertNull(uri.extractImageId())
    }

    @Test
    fun `non content uri yields null`() {
        val uri = Uri.parse("https://example.com/images/123.jpg")

        assertNull(uri.extractImageId())
    }
}
