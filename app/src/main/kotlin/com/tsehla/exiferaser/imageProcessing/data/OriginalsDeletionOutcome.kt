package com.tsehla.exiferaser.imageProcessing.data

import android.app.PendingIntent

sealed interface OriginalsDeletionOutcome {
    data object Deleted : OriginalsDeletionOutcome
    data class ConsentRequired(val pendingIntent: PendingIntent) : OriginalsDeletionOutcome
    data object Failed : OriginalsDeletionOutcome
}
