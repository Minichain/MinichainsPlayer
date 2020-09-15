package com.example.minichainsplayer

import android.content.res.Resources

class Utils {
    companion object {
        fun millisecondsToHoursMinutesAndSeconds(milliseconds: Long?): String {
            if (milliseconds == null || milliseconds <= 0) return "0:00"
            val seconds = Math.floor((milliseconds / 1000.0) % 60).toInt()
            val minutes = Math.floor(((milliseconds / (1000.0 * 60.0)) % 60)).toInt()
            val hours = Math.floor((milliseconds / (1000.0 * 60.0 * 60.0)) % 24).toInt()
            var returnString = String()

            //hours
            if (hours > 0) {
                if (hours < 10) {
                    returnString = returnString.plus(0)
                }
                returnString = returnString.plus(hours).plus(":")
            }

            //minutes
            if (minutes < 10 && hours > 0) {
                returnString = returnString.plus(0)
            }
            returnString = returnString.plus(minutes).plus(":")

            //seconds
            if (seconds < 10) {
                returnString = returnString.plus(0)
            }
            returnString = returnString.plus(seconds)

            return returnString
        }

        fun dpToPx(dp: Float): Int {
            return (dp * Resources.getSystem().displayMetrics.density).toInt()
        }

        fun pxToDp(px: Int): Float {
            return (px.toFloat() / Resources.getSystem().displayMetrics.density)
        }
    }
}