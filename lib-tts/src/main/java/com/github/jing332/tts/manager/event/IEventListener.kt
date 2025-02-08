package com.github.jing332.tts.manager.event

fun interface IEventListener {
    fun onEvent(event: EventType)
}