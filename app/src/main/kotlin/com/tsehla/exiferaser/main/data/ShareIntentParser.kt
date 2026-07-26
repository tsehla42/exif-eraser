package com.tsehla.exiferaser.main.data

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.tsehla.exiferaser.core.extension.supportedImageUrisToList

object ShareIntentParser {

    const val EXTRA_SHARE_CONSUMED = "com.tsehla.exiferaser.extra.SHARE_CONSUMED"

    fun isShareAction(action: String?): Boolean =
        action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE

    fun parse(intent: Intent): List<Uri>? {
        if (!isShareAction(intent.action)) return null
        if (intent.getBooleanExtra(EXTRA_SHARE_CONSUMED, false)) return null
        val clipData = intent.clipData
        if (clipData != null && clipData.itemCount > 0) {
            return clipData.supportedImageUrisToList()
        }
        return intent.extraStreamUris()
    }

    private fun Intent.extraStreamUris(): List<Uri> {
        IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            ?.let { return listOf(it) }
        IntentCompat.getParcelableArrayListExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            ?.let { return it }
        return emptyList()
    }
}
