package com.github.jing332.database.entities.systts.v1.tts

import android.os.Parcelable
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.BaseAudioFormat
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.InputStream

@Parcelize
@Serializable
@SerialName("internal")
data class MsTTS(
    var api: Int = MsTtsApiType.EDGE,
    var format: String = MsTtsAudioFormat.DEFAULT,
    override var locale: String = DEFAULT_LOCALE,
    // 二级语言（语言技能）仅限en-US-JennyMultilingualNeural
    var secondaryLocale: String? = null,
    var voiceName: String = DEFAULT_VOICE,
    var voiceId: String? = null,
    var prosody: Prosody = Prosody(),
    var expressAs: ExpressAs? = null,

    override var audioPlayer: PlayerParams = PlayerParams(),
    override var audioParams: AudioParams = AudioParams(),
    @Transient
    override var audioFormat: BaseAudioFormat = MsTtsFormatManger.getFormatOrDefault(format),
    @Transient
    override var speechRule: SpeechRuleInfo = SpeechRuleInfo(),
) : Parcelable, ITextToSpeechEngine() {
    companion object {
        const val RATE_FOLLOW_SYSTEM = -100
        const val PITCH_FOLLOW_SYSTEM = -50

        const val DEFAULT_LOCALE = "zh-CN"
        const val DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"
    }

    @IgnoredOnParcel
    override var pitch: Int
        get() {
            return prosody.pitch
        }
        set(value) {
            prosody.pitch = value
        }

    @IgnoredOnParcel
    override var volume: Int
        get() {
            return prosody.volume
        }
        set(value) {
            prosody.volume = value
        }

    @IgnoredOnParcel
    override var rate: Int
        get() = prosody.rate
        set(value) {
            prosody.rate = value
        }

    override fun isRateFollowSystem(): Boolean {
        return RATE_FOLLOW_SYSTEM == rate
    }

    override fun isPitchFollowSystem(): Boolean {
        return PITCH_FOLLOW_SYSTEM == pitch
    }

    override fun getDescription(): String = ""

    @IgnoredOnParcel
    private var lastLoadTime: Long = 0

    override fun onLoad() {

    }

    override fun getType(): String {
        return ""
    }

    override fun getBottomContent(): String = ""


    override fun onStop() {
    }


    override suspend fun getAudio(speakText: String, rate: Int, pitch: Int): InputStream {
        TODO()
    }

//    override suspend fun getAudioStream(
//        speakText: String,
//        chunkSize: Int,
//        onData: (ByteArray?) -> Unit
//    ) {
//        SysTtsLib.getAudioStream(speakText, this@MsTTS) {
//            onData(it)
//        }
//    }
}

@Serializable
@Parcelize
data class ExpressAs(
    var style: String? = null,
    var styleDegree: Float = 1F,
    var role: String? = null
) : Parcelable {
    constructor() : this("", 1F, "")
}

/* Prosody 基本数值参数 单位: %百分比 */
@Serializable
@Parcelize
data class Prosody(
    var rate: Int = MsTTS.RATE_FOLLOW_SYSTEM,
    var volume: Int = 0,
    var pitch: Int = MsTTS.PITCH_FOLLOW_SYSTEM
) : Parcelable