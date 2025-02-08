package com.github.jing332.tts.manager

interface IBgmPlayer {
    fun onPlay()
    fun onStop()
    fun onDestroy()
    fun setPlayList(
        list: List<Pair<String, Float>>
    )
}