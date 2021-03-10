package com.minichain.minichainsplayer

import android.content.res.Resources

class AppTheme {
    companion object {
        fun getColorAccordingToTheme(r: Resources, color: String, theme: Int): Int {
            when (color) {
                "white" -> {
                    return if (theme == R.id.app_theme_01) {
                        r.getColor(R.color.white)
                    } else {
                        r.getColor(R.color.dark_theme_white)
                    }
                }
                "text_01" -> {
                    return if (theme == R.id.app_theme_01) {
                        r.getColor(R.color.text_01)
                    } else {
                        r.getColor(R.color.dark_theme_text_01)
                    }
                }
                "hint" -> {
                    return if (theme == R.id.app_theme_01) {
                        r.getColor(R.color.hint)
                    } else {
                        r.getColor(R.color.dark_theme_hint)
                    }
                }
                "background_01" -> {
                    return if (theme == R.id.app_theme_01) {
                        r.getColor(R.color.background_01)
                    } else {
                        r.getColor(R.color.dark_theme_background_01)
                    }
                }
                "background_02" -> {
                    return if (theme == R.id.app_theme_01) {
                        r.getColor(R.color.background_02)
                    } else {
                        r.getColor(R.color.dark_theme_background_02)
                    }
                }
                "background_03" -> {
                    return if (theme == R.id.app_theme_01) {
                        r.getColor(R.color.background_03)
                    } else {
                        r.getColor(R.color.dark_theme_background_03)
                    }
                }
                else -> return -1
            }
        }
    }
}