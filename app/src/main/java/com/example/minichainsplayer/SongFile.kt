package com.example.minichainsplayer

class SongFile {
    var path: String
    var songName: String
    var length: Long = 0

    constructor(path: String, songName: String, length: Long) {
        this.path = path
        this.songName = songName
        this.length = length
    }
}