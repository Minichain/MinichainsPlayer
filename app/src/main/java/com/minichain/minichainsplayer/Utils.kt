package com.minichain.minichainsplayer

import android.content.res.Resources

class Utils {
    companion object {
        fun millisecondsToHoursMinutesAndSeconds(milliseconds: Int?): String {
            return millisecondsToHoursMinutesAndSeconds(milliseconds?.toLong());
        }

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

        fun normalize(samples: FloatArray): FloatArray {
            val index = max(samples)
            if (index != -1 && index < samples.size) {
                val maxValue = samples[index]
                if (maxValue > 0f) {
                    for (i in samples.indices step 1) {
                        samples[i] = samples[i] / maxValue
                    }
                }
            }
            return samples
        }

        fun max(array: FloatArray): Int {
            var maxValue: Float = -1f
            var index: Int = -1
            for (i in array.indices step 1) {
                if (array[i] > maxValue) {
                    maxValue = array[i]
                    index = i
                }
            }
            return index
        }

        fun smooth(array: FloatArray): FloatArray {
            var newArray = FloatArray(array.size)
            newArray[0] = array[0]
            newArray[array.size - 1] = array[array.size - 1]
            for (i in 1 until (array.size - 1) step 1) {
                newArray[i] = average(floatArrayOf(array[i - 1], array[i], array[i + 1]))
            }
            return newArray
        }

        fun average(array: FloatArray): Float {
            var sum = 0f
            for (i in array.indices step 1) {
                sum += array[i]
            }
            return sum / array.size
        }
    }
}