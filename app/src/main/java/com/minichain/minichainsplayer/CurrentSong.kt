package com.minichain.minichainsplayer

class CurrentSong {
    var playing: Boolean
    var currentSongInteger: Int
    var currentSongLength: Int
    var currentSongPath: String
    var currentSongName: String
    var currentSongTime: Int

    constructor(currentSongInteger: Int = 0, currentSongLength: Int = 0, currentSongName: String = "",
                currentSongPath: String = "", currentSongTime: Int = 0, playing: Boolean = false) {
        this.currentSongInteger = currentSongInteger
        this.currentSongLength = currentSongLength
        this.currentSongName = currentSongName
        this.currentSongPath = currentSongPath
        this.currentSongTime = currentSongTime
        this.playing = playing
    }
}