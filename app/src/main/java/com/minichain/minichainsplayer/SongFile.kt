package com.minichain.minichainsplayer

class SongFile {
    var path: String
    var songName: String
    var format: String
    var length: Int = 0

    constructor(path: String, songName: String, format: String, length: Int) {
        this.path = path
        this.songName = songName
        this.format = format
        this.length = length
    }
}