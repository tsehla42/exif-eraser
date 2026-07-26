package com.none.tom.exiferaser.metadataViewer.data

import android.content.Context
import android.net.Uri
import com.none.tom.exiferaser.core.di.DispatcherIo
import com.none.tom.exiferaser.core.extension.suspendRunCatching
import com.drew.imaging.MetadataReader
import com.drew.metadata.Metadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class MetadataGroup(
    val name: String,
    val tags: List<MetadataTag>,
)

data class MetadataTag(
    val tagName: String,
    val value: String,
    val description: String,
)

@Singleton
class MetadataViewerRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:DispatcherIo private val dispatcherIo: CoroutineDispatcher,
) {
    suspend fun readMetadata(uri: Uri): Result<List<MetadataGroup>> =
        withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val metadata: Metadata = context.contentResolver.openInputStream(uri)?.use { stream ->
                    MetadataReader.readMetadata(stream)
                } ?: throw IllegalStateException("Could not open input stream for $uri")

                metadata.directories
                    .filter { it.tagCount > 0 }
                    .map { directory ->
                        MetadataGroup(
                            name = directory.name,
                            tags = directory.tags.map { tag ->
                                MetadataTag(
                                    tagName = tag.tagName,
                                    value = tag.value.toString(),
                                    description = tag.description ?: tag.value.toString(),
                                )
                            }.sortedBy { it.tagName },
                        )
                    }
                    .sortedBy { it.name }
            }
        }
}
