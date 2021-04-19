package com.minichain.minichainsplayer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class MyGLSurfaceView : GLSurfaceView {
    private val renderer: MyGLRenderer

    constructor(context: Context, attributes: AttributeSet? = null): super(context) {
        setEGLContextClientVersion(2)
        val backgroundColor = context.resources.getColor(R.color.color_04)
        val primaryColor = context.resources.getColor(R.color.color_02)
        renderer = MyGLRenderer(backgroundColor, primaryColor)
        setRenderer(renderer)
    }

    fun getRenderer(): MyGLRenderer {
        return renderer
    }
}