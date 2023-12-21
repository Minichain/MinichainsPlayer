//package com.minichain.minichainsplayer
//
//import android.content.Context
//import android.opengl.GLSurfaceView
//import android.util.AttributeSet
//
//class MyGLSurfaceView(context: Context, attributes: AttributeSet? = null) : GLSurfaceView(context) {
//  private val renderer: MyGLRenderer
//
//  init {
//    setEGLContextClientVersion(2)
//    val backgroundColor = context.resources.getColor(R.color.color_04)
//    val primaryColor = context.resources.getColor(R.color.color_02)
//    renderer = MyGLRenderer(backgroundColor, primaryColor)
//    setRenderer(renderer)
//  }
//
//  fun getRenderer(): MyGLRenderer {
//    return renderer
//  }
//}