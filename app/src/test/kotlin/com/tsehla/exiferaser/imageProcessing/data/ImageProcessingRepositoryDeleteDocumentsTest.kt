package com.tsehla.exiferaser.imageProcessing.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImageProcessingRepositoryDeleteDocumentsTest {

    private class RecordingProvider : ContentProvider() {
        var deleteCalls = 0

        override fun onCreate(): Boolean = true

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            deleteCalls++
            return 1
        }

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    private fun createRepository(context: Context) = ImageProcessingRepository(
        context = context,
        dispatcherIo = Dispatchers.Unconfined,
    )

    @Test
    fun `deleteDocuments deletes media store content uri via content resolver`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val provider = RecordingProvider()
        ShadowContentResolver.registerProviderInternal("media", provider)
        val repository = createRepository(context)

        val result = repository.deleteDocuments(
            listOf(Uri.parse("content://media/external/images/media/5"))
        )

        assertTrue(result.isSuccess)
        assertEquals(1, provider.deleteCalls)
    }
}
