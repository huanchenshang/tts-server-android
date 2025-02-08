//package com.github.jing332.tts.manager
//
//sealed class Error {
//
//}
//
//sealed class Result<out T> {
//
//    open class Error : Result<Any>()
//    open class Success<T>(val data: T) : Result<T>()
//
//    data object SuccessUnit : Success<Unit>(Unit)
//
//    data object ErrConfigEmpty : Error()
//    data object ErrGetTts : Error()
//    data object ErrGetBGM : Error()
//    data class ErrGetSpeechRule(val ruleId: String) : Error()
//    data class ErrForceConfigNotFound(val forceConfigId: Long) : Error()
//}