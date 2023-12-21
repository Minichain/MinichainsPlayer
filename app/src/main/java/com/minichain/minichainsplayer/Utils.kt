package com.minichain.minichainsplayer

import android.content.res.Resources
import kotlin.math.floor

fun Int.millisecondsToHoursMinutesAndSeconds(): String =
  toLong().millisecondsToHoursMinutesAndSeconds()

fun Long.millisecondsToHoursMinutesAndSeconds(): String {
  if (this <= 0) return "0:00"
  val seconds = floor((this / 1000.0) % 60).toInt()
  val minutes = floor(((this / (1000.0 * 60.0)) % 60)).toInt()
  val hours = floor((this / (1000.0 * 60.0 * 60.0)) % 24).toInt()
  var returnString = String()

  if (hours > 0) {
    if (hours < 10) returnString = returnString.plus(0)
    returnString = returnString.plus(hours).plus(":")
  }

  if (minutes < 10 && hours > 0) returnString = returnString.plus(0)
  returnString = returnString.plus(minutes).plus(":")

  if (seconds < 10) returnString = returnString.plus(0)
  returnString = returnString.plus(seconds)

  return returnString
}

fun dpToPx(dp: Float): Int =
  (dp * Resources.getSystem().displayMetrics.density).toInt()

fun pxToDp(px: Int): Float =
  (px.toFloat() / Resources.getSystem().displayMetrics.density)

fun FloatArray.normalize(): FloatArray {
  val index = this.max()
  if (index != -1 && index < size) {
    val maxValue = get(index)
    if (maxValue > 0f) {
      for (i in indices step 1) {
        this[i] = get(i) / maxValue
      }
    }
  }
  return this
}

fun FloatArray.max(): Int {
  var maxValue: Float = -1f
  var index: Int = -1
  for (i in indices step 1) {
    if (get(i) > maxValue) {
      maxValue = get(i)
      index = i
    }
  }
  return index
}

fun FloatArray.smooth(): FloatArray {
  val newArray = FloatArray(size)
  newArray[0] = get(0)
  newArray[size - 1] = get(size - 1)
  for (i in 1 until (size - 1) step 1) {
    newArray[i] = floatArrayOf(get(i - 1), get(i), get(i + 1)).average()
  }
  return newArray
}

fun FloatArray.average(): Float {
  var sum = 0f
  for (i in indices step 1) {
    sum += get(i)
  }
  return sum / size
}