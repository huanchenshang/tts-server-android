package com.github.jing332.tts_server_android.conf

import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.app

object SystemTtsConfig {

    private val dataSaverPref = DataSaverPreferences(app.getSharedPreferences("systts", 0))

    val isInternalPlayerEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isInAppPlayAudio",
        initialValue = false
    )

    val inAppPlaySpeed = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "inAppPlaySpeed",
        initialValue = 1f
    )

    val inAppPlayVolume = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "inAppPlayVolume",
        initialValue = 1f
    )

    val inAppPlayPitch = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "inAppPlayPitch",
        initialValue = 1f
    )

    val audioParamsSpeed = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "audioParamsSpeed",
        initialValue = 1f
    )

    val audioParamsPitch = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "audioParamsPitch",
        initialValue = 1f
    )

    val audioParamsVolume = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "audioParamsVolume",
        initialValue = 1f
    )

    val bgmVolume = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "bgmVolume",
        initialValue = 1f
    )

    val isBgmShuffleEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isBgmShuffleEnabled",
        initialValue = false
    )

    val isMultiVoiceEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isMultiVoiceEnabled",
        initialValue = false
    )

    val isVoiceMultipleEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isVoiceMultipleEnabled",
        initialValue = false
    )

    val isGroupMultipleEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isGroupMultipleEnabled",
        initialValue = false
    )

    val isWakeLockEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isWakeLockEnabled",
        initialValue = true
    )

    val isForegroundServiceEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isForegroundServiceEnabled",
        initialValue = true
    )

    val isReplaceEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isReplaceEnabled",
        initialValue = false
    )

    val isSplitEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isSplitEnabled",
        initialValue = false
    )

    val requestTimeout = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "requestTimeout",
        initialValue = 5000
    )

    val maxRetryCount = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "maxRetryCount",
        initialValue = 3
    )

    val standbyTriggeredRetryIndex = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "standbyTriggeredRetryIndex",
        initialValue = 1
    )

    val maxEmptyAudioRetryCount = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "maxEmptyAudioRetryCount",
        initialValue = 1
    )

    val isSkipSilentText = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isSkipSilentText",
        initialValue = true
    )

    val isStreamPlayModeEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isStreamPlayModeEnabled",
        initialValue = true
    )

    val isExoDecoderEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isExoDecoderEnabled",
        initialValue = true
    )

    val isSilenceSkipAudio = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isSilenceSkipAudio",
        initialValue = false
    )

}