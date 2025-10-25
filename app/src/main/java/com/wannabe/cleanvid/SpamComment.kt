package com.wannabe.cleanvid

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SpamComment(
    val id: String,
    val text: String,
    val authorChannelId: String,
    var isChecked: Boolean = true
) : Parcelable