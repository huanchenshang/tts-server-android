package com.github.jing332.tts.manager.event

fun interface IEventDispatcher {
    fun dispatch(event: Event)
}