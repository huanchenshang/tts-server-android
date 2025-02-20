package com.github.jing332.database.entities.systts.source

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator


@Parcelize
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("#type")
sealed class TextToSpeechSource : Parcelable {

    open fun isSyncPlayMode(): Boolean = false

    open fun getKey(): String {
        return javaClass.simpleName
    }

    abstract val locale: String
    abstract val voice: String
}