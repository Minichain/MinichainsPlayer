package com.minichain.minichainsplayer

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLES32.GL_QUADS
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.math.sin

class Rectangle {
    private var xPosition: Float = 0f
    private var yPosition: Float = 0f
    private var width: Float = 0f
    private var height: Float = 0f

    private var oscillator: Float = Math.random().toFloat()

    private val COORDS_PER_VERTEX = 3
    private var verticesCoords: FloatArray = floatArrayOf(
        0.0f, 0.0f, 0.0f,      // top left
        0.0f, 0.0f, 0.0f,      // bottom left
        0.0f, 0.0f, 0.0f,      // bottom right
        0.0f, 0.0f, 0.0f       // top right
    )

    private var verticesCount: Int = 0

    private lateinit var vertexBuffer: FloatBuffer
    private var drawListBuffer: ShortBuffer
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    private var color: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)

    constructor(x: Float, y: Float, width: Float, height: Float, color: Int) {
        xPosition = x
        yPosition = y
        this.width = width
        this.height = height
        setVertices(xPosition, yPosition, width, height)

        this.color[0] = Color.red(color) / 256f
        this.color[1] = Color.green(color) / 256f
        this.color[2] = Color.blue(color) / 256f
        this.color[3] = 0.25f

        verticesCount = verticesCoords.size / COORDS_PER_VERTEX

        drawListBuffer =
            // (# of coordinate values * 2 bytes per short)
            ByteBuffer.allocateDirect(drawOrder.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(drawOrder)
                    position(0)
                }
            }
    }

    private fun setVertices(x: Float, y: Float, width: Float, height: Float) {
        verticesCoords[0] = x
        verticesCoords[1] = y + height / 2f
        verticesCoords[2] = 0f

        verticesCoords[3] = x
        verticesCoords[4] = y
        verticesCoords[5] = 0f

        verticesCoords[6] = x + width / 2f
        verticesCoords[7] = y
        verticesCoords[8] = 0f

        verticesCoords[9] = x + width / 2f
        verticesCoords[10] = y + height / 2f
        verticesCoords[11] = 0f

        vertexBuffer =
            ByteBuffer.allocateDirect(verticesCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(verticesCoords)
                    position(0)
                }
            }
    }

    fun update(height: Float) {
//        oscillator += 0.1f
//        this.height = sin(oscillator) * 0.5f + 0.5f
        this.height -= (this.height - height) / 25f
        setVertices(xPosition, yPosition, width, this.height)
    }


    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    fun draw(mProgram: Int) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(it, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, COORDS_PER_VERTEX * 4, vertexBuffer)

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->
                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, verticesCount)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

            GLES20.glDisableVertexAttribArray(it)
        }
    }
}