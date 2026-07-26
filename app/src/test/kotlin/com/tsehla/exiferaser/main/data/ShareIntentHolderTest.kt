package com.tsehla.exiferaser.main.data

import android.net.Uri
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentHolderTest {

    private val holder = ShareIntentHolder()

    @Test
    fun `submitted uris are exposed until consumed`() = runTest {
        val uris = listOf(Uri.parse("content://media/images/1"), Uri.parse("content://media/images/2"))
        holder.uris.test {
            assertNull(awaitItem())
            holder.submit(uris)
            assertEquals(uris, awaitItem())
            holder.consume()
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit replaces previously submitted uris`() = runTest {
        val first = listOf(Uri.parse("content://media/images/1"))
        val second = listOf(Uri.parse("content://media/images/2"))
        holder.uris.test {
            assertNull(awaitItem())
            holder.submit(first)
            assertEquals(first, awaitItem())
            holder.submit(second)
            assertEquals(second, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submitting empty list is exposed as empty list not null`() = runTest {
        holder.uris.test {
            assertNull(awaitItem())
            holder.submit(emptyList())
            assertEquals(emptyList<Uri>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
