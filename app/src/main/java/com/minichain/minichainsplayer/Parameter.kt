package com.minichain.minichainsplayer

object Parameter {
    var shuffle = false

    fun init() {
        Log.l("Loading Parameters... ")

        shuffle = loadShuffle()

        Log.l("Parameters Loaded!")
    }

    private fun loadShuffle(): Boolean {
        var shuffle = false
        try {
            shuffle = DataBase.getParameter("shuffle").toBoolean()
            Log.l("loading shuffle:: shuffle: " + shuffle)
        } catch (e: Exception) {
            setParameter("shuffle", false.toString())
        }
        return shuffle
    }

    fun setParameter(parameter: String, value: String) {
        DataBase.setParameter(parameter, value)
    }
}