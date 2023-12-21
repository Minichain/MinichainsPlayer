package com.minichain.minichainsplayer

sealed class Event
object PlayStopSong: Event()
object PreviousSong: Event()
object NextSong: Event()
