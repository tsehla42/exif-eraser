package com.tsehla.exiferaser.metadataViewer.data

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.tsehla.exiferaser.core.di.DispatcherIo
import com.tsehla.exiferaser.core.extension.runCatchingResult
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.parcelize.Parcelize

@Parcelize
data class MetadataGroup(
    val name: String,
    val tags: List<MetadataTag>,
) : Parcelable

@Parcelize
data class MetadataTag(
    val tagName: String,
    val value: String,
    val description: String,
) : Parcelable

@Singleton
class MetadataViewerRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:DispatcherIo private val dispatcherIo: CoroutineDispatcher,
) {
    suspend fun readMetadata(uri: Uri): Result<List<MetadataGroup>> =
        dispatcherIo.runCatchingResult {
            val metadata: Metadata = context.contentResolver.openInputStream(uri)?.use { stream ->
                ImageMetadataReader.readMetadata(stream)
            } ?: throw IllegalStateException("Could not open input stream for $uri")

            metadata.directories
                .filter { it.tagCount > 0 }
                .map { directory ->
                    MetadataGroup(
                        name = directory.name,
                        tags = directory.tags.map { tag ->
                            MetadataTag(
                                tagName = tag.tagName,
                                value = tag.description ?: tag.tagName,
                                description = tag.description ?: tag.tagName,
                            )
                        }.sortedBy { it.tagName },
                    )
                }
                .sortedBy { it.name }
        }
}
