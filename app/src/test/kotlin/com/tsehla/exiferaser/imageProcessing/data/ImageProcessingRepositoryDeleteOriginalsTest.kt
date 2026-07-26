package com.tsehla.exiferaser.imageProcessing.data

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.IntentFilter
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowPackageManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImageProcessingRepositoryDeleteOriginalsTest {

    private fun createRepository(context: android.content.Context) = ImageProcessingRepository(
        context = context,
        dispatcherIo = Dispatchers.Unconfined,
    )

    /**
     * A [ContentProvider] standing in for the system MediaStore: it resolves image rows by
     * path (ignoring the selection) and refuses deletes the way MediaStore does on Android 9
     * for apps without WRITE_EXTERNAL_STORAGE (SecurityException).
     */
    private class MediaStoreLikeProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = MatrixCursor(arrayOf(BaseColumns._ID)).apply { addRow(arrayOf("42")) }

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
            throw SecurityException("Permission Denial: requires android.permission.WRITE_EXTERNAL_STORAGE")

        override fun getType(uri: Uri): String? = "image/jpeg"

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    /** A permissive MediaStore stand-in whose deletes succeed (pre-R direct delete path). */
    private class DeletingMediaProvider : ContentProvider() {
        var deleteCalls = 0

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            deleteCalls++
            return 1
        }

        override fun getType(uri: Uri): String? = "image/jpeg"

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    /**
     * Makes [DocumentsContract.isDocumentUri] recognize the externalstorage documents
     * provider, which is not part of the app manifest Robolectric installs.
     */
    private fun registerExternalStorageDocumentsProvider(context: android.content.Context) {
        val shadowPackageManager = Shadow.extract<ShadowPackageManager>(context.packageManager)
        val component = ComponentName(context.packageName, "test.FakeExternalStorageProvider")
        val providerInfo = shadowPackageManager.addProviderIfNotPresent(component)
        providerInfo.authority = "com.android.externalstorage.documents"
        providerInfo.exported = true
        shadowPackageManager.addOrUpdateProvider(providerInfo)
        shadowPackageManager.addIntentFilterForProvider(
            component,
            // DocumentsContract.ACTION_DOCUMENTS_PROVIDER (no public constant exposed)
            IntentFilter("android.content.action.DOCUMENTS_PROVIDER"),
        )
    }

    @Test
    fun `deleteOriginals deletes app owned cache file without user consent`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val directory = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val file = File(directory, "shot.jpg").apply { writeText("photo") }
        val uri = FileProvider.getUriForFile(
            context,
            com.tsehla.exiferaser.core.provider.CameraFileProvider.AUTHORITY,
            file,
        )

        val outcome = createRepository(context).deleteOriginals(listOf(uri))

        assertEquals(OriginalsDeletionOutcome.Deleted, outcome)
        assertFalse("app owned cache file should be deleted", file.exists())
    }

    @Test
    fun `deleteOriginals returns Failed for foreign file provider uri`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val foreignUri = Uri.parse("content://org.other.app.provider/images/1.jpg")

        val outcome = createRepository(context).deleteOriginals(listOf(foreignUri))

        assertEquals(OriginalsDeletionOutcome.Failed, outcome)
    }

    @Test
    @Config(sdk = [28])
    fun `deleteOriginals deletes saf document original through document file on pre-R`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        registerExternalStorageDocumentsProvider(context)
        ShadowContentResolver.registerProviderInternal("media", MediaStoreLikeProvider())
        val documentUri = Uri.parse(
            "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FIMG_0001.jpg",
        )

        // Precondition: Robolectric must recognize the SAF documents provider for the
        // document branch of deleteOriginals to be reachable at all.
        assertEquals(true, DocumentsContract.isDocumentUri(context, documentUri))

        val outcome = createRepository(context).deleteOriginals(listOf(documentUri))

        assertEquals(OriginalsDeletionOutcome.Deleted, outcome)
    }

    @Test
    @Config(sdk = [28])
    fun `deleteOriginals deletes media store uri directly on pre-R without consent`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val mediaProvider = DeletingMediaProvider()
        ShadowContentResolver.registerProviderInternal("media", mediaProvider)
        val mediaUri = Uri.parse("content://media/external/images/media/42")

        val outcome = createRepository(context).deleteOriginals(listOf(mediaUri))

        assertEquals(OriginalsDeletionOutcome.Deleted, outcome)
        assertEquals(1, mediaProvider.deleteCalls)
    }
}
