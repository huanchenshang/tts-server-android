package com.github.jing332.tts.manager

data class StandbyInfo(
//    val id: Long,

    // use standby tts when try number of times
    val tryTimesWhenTrigger: Int = 1,
    val config: TtsConfiguration? = null,
) {

}