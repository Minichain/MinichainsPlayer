package com.minichain.minichainsplayer

import android.graphics.Color
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView

class MyGLRenderer : GLSurfaceView.Renderer {
    private var myRectangles = ArrayList<Rectangle>()
    private var spectrum: FloatArray = FloatArray(10)

    private var mProgram: Int = 0

    private val vertexShaderCode =
        "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    private var backgroundColor: Int = 0
    private var primaryColor: Int = 0

    constructor(backgroundColor: Int, primaryColor: Int) {
        this.backgroundColor = backgroundColor
        this.primaryColor = primaryColor
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor((Color.red(backgroundColor) / 256f), (Color.green(backgroundColor) / 256f), (Color.blue(backgroundColor) / 256f), 1.0f)

        for (i in 0 until 10 step 1) {
            myRectangles.add(Rectangle((i * 0.2f) - 0.95f, -1.0f, 0.2f, 0f, primaryColor))
        }

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram().also {
            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        for (i in 0 until 10 step 1) {
            myRectangles[i].update(spectrum[i])
            myRectangles[i].draw(mProgram)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        return GLES20.glCreateShader(type).also { shader ->
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun setSpectrum(spectrum: FloatArray) {
        this.spectrum = spectrum
    }
}